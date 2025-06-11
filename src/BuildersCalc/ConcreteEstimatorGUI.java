// ConcreteEstimatorGUI.java - Full implementation with PDF + CSV export, real-time cost update, pretty UI

package BuildersCalc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.Properties;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

public class ConcreteEstimatorGUI extends JFrame {
    private final JTextField lengthField, widthField, depthField, quantityField;
    private final JComboBox<String> lengthUnit, widthUnit, depthUnit;
    private final JTextField cementBagsField, sandBagsField, aggregateBagsField, volumeField;
    private final JTextField cementCostField, sandCostField, aggregateCostField, totalCostField;
    private final JButton calculateButton;

    private final Properties lastValues = new Properties();
    private final File saveFile = new File("concrete_estimator.properties");

    public ConcreteEstimatorGUI() {
        setTitle("Concrete Material Estimator - MD Harrington Kent DA6 8NP");
        setSize(600, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 20, 5, 20);

        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {}

        String[] units = {"m", "cm", "inches"};

        int y = 0;
        addField(gbc, y++, "Length:", lengthField = new JTextField(10), lengthUnit = new JComboBox<>(units));
        addField(gbc, y++, "Width:", widthField = new JTextField(10), widthUnit = new JComboBox<>(units));
        addField(gbc, y++, "Depth:", depthField = new JTextField(10), depthUnit = new JComboBox<>(units));
        addField(gbc, y++, "Quantity of Blocks:", quantityField = new JTextField("1", 10), null);
        addField(gbc, y++, "Volume (m³ / ft³):", volumeField = createOutputField(), null);
        addField(gbc, y++, "Cement Bags (25kg):", cementBagsField = createOutputField(), null);
        addField(gbc, y++, "Sand Bags (25kg):", sandBagsField = createOutputField(), null);
        addField(gbc, y++, "Aggregate Bags (25kg):", aggregateBagsField = createOutputField(), null);
        addField(gbc, y++, "Cement Cost per Bag (£):", cementCostField = new JTextField("5.00", 10), null);
        addField(gbc, y++, "Sand Cost per Bag (£):", sandCostField = new JTextField("3.00", 10), null);
        addField(gbc, y++, "Aggregate Cost per Bag (£):", aggregateCostField = new JTextField("4.00", 10), null);
        addField(gbc, y++, "Total Estimated Cost (£):", totalCostField = createOutputField(), null);

        calculateButton = new JButton("Calculate");
        gbc.gridx = 1; gbc.gridy = y++;
        add(calculateButton, gbc);

        calculateButton.addActionListener(e -> calculateMaterials());

        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { calculateMaterials(); }
            public void removeUpdate(DocumentEvent e) { calculateMaterials(); }
            public void changedUpdate(DocumentEvent e) { calculateMaterials(); }
        };

        for (JTextField field : new JTextField[]{lengthField, widthField, depthField, quantityField, cementCostField, sandCostField, aggregateCostField})
            field.getDocument().addDocumentListener(listener);

        loadLastValues();
        setupMenuBar();
        setVisible(true);
    }

    private void addField(GridBagConstraints gbc, int row, String label, JTextField field, JComboBox<String> unitBox) {
        gbc.gridx = 0; gbc.gridy = row; add(new JLabel(label), gbc);
        gbc.gridx = 1; add(field, gbc);
        if (unitBox != null) { gbc.gridx = 2; add(unitBox, gbc); }
    }

    private JTextField createOutputField() {
        JTextField field = new JTextField(10);
        field.setEditable(false);
        return field;
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem exportPdf = new JMenuItem("Export as PDF");
        exportPdf.addActionListener(e -> exportToPDF());
        fileMenu.add(exportPdf);

        JMenuItem exportCsv = new JMenuItem("Export as CSV");
        exportCsv.addActionListener(e -> exportToCSV());
        fileMenu.add(exportCsv);

        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private double convertToMeters(double value, String unit) {
        return switch (unit) {
            case "cm" -> value / 100.0;
            case "inches" -> value * 0.0254;
            default -> value;
        };
    }

    private void calculateMaterials() {
        try {
            double length = convertToMeters(Double.parseDouble(lengthField.getText()), (String) lengthUnit.getSelectedItem());
            double width = convertToMeters(Double.parseDouble(widthField.getText()), (String) widthUnit.getSelectedItem());
            double depth = convertToMeters(Double.parseDouble(depthField.getText()), (String) depthUnit.getSelectedItem());
            int qty = Integer.parseInt(quantityField.getText());

            double volume = length * width * depth * qty;
            double volumeFt3 = volume * 35.3147;
            int cementBags = (int) Math.round(volume * 6.1);
            int sandBags = (int) Math.round(volume * 1600 * 0.44 / 25.0);
            int aggregateBags = (int) Math.round(volume * 1500 * 0.88 / 25.0);

            volumeField.setText(String.format("%.3f / %.2f", volume, volumeFt3));
            cementBagsField.setText(String.valueOf(cementBags));
            sandBagsField.setText(String.valueOf(sandBags));
            aggregateBagsField.setText(String.valueOf(aggregateBags));

            double totalCost = cementBags * Double.parseDouble(cementCostField.getText())
                             + sandBags * Double.parseDouble(sandCostField.getText())
                             + aggregateBags * Double.parseDouble(aggregateCostField.getText());
            totalCostField.setText(String.format("%.2f", totalCost));

            saveValues();
        } catch (NumberFormatException ex) {
            totalCostField.setText("--");
        }
    }

    private void exportToPDF() {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream("Concrete_Estimate.pdf"));
            document.open();

            document.add(new Paragraph("Concrete Estimation Summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(2);
            table.addCell("Length"); table.addCell(lengthField.getText() + " " + lengthUnit.getSelectedItem());
            table.addCell("Width"); table.addCell(widthField.getText() + " " + widthUnit.getSelectedItem());
            table.addCell("Depth"); table.addCell(depthField.getText() + " " + depthUnit.getSelectedItem());
            table.addCell("Quantity"); table.addCell(quantityField.getText());
            table.addCell("Volume"); table.addCell(volumeField.getText());
            table.addCell("Cement Bags"); table.addCell(cementBagsField.getText());
            table.addCell("Sand Bags"); table.addCell(sandBagsField.getText());
            table.addCell("Aggregate Bags"); table.addCell(aggregateBagsField.getText());
            table.addCell("Total Cost"); table.addCell("£" + totalCostField.getText());

            document.add(table);
            document.close();

            JOptionPane.showMessageDialog(this, "PDF exported successfully.");
        } catch (DocumentException | HeadlessException | FileNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Error exporting PDF.");
        }
    }

    private void exportToCSV() {
        try (PrintWriter writer = new PrintWriter(new File("Concrete_Estimate.csv"))) {
            writer.println("Field,Value");
            writer.println("Length," + lengthField.getText() + " " + lengthUnit.getSelectedItem());
            writer.println("Width," + widthField.getText() + " " + widthUnit.getSelectedItem());
            writer.println("Depth," + depthField.getText() + " " + depthUnit.getSelectedItem());
            writer.println("Quantity," + quantityField.getText());
            writer.println("Volume," + volumeField.getText());
            writer.println("Cement Bags," + cementBagsField.getText());
            writer.println("Sand Bags," + sandBagsField.getText());
            writer.println("Aggregate Bags," + aggregateBagsField.getText());
            writer.println("Total Cost," + totalCostField.getText());
            JOptionPane.showMessageDialog(this, "CSV exported successfully.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error exporting CSV.");
        }
    }

    private void saveValues() {
        try (FileOutputStream out = new FileOutputStream(saveFile)) {
            lastValues.setProperty("length", lengthField.getText());
            lastValues.setProperty("width", widthField.getText());
            lastValues.setProperty("depth", depthField.getText());
            lastValues.setProperty("quantity", quantityField.getText());
            lastValues.setProperty("cementCost", cementCostField.getText());
            lastValues.setProperty("sandCost", sandCostField.getText());
            lastValues.setProperty("aggregateCost", aggregateCostField.getText());
            lastValues.store(out, null);
        } catch (IOException ignored) {}
    }

    private void loadLastValues() {
        if (saveFile.exists()) {
            try (FileInputStream in = new FileInputStream(saveFile)) {
                lastValues.load(in);
                lengthField.setText(lastValues.getProperty("length", ""));
                widthField.setText(lastValues.getProperty("width", ""));
                depthField.setText(lastValues.getProperty("depth", ""));
                quantityField.setText(lastValues.getProperty("quantity", "1"));
                cementCostField.setText(lastValues.getProperty("cementCost", "5.00"));
                sandCostField.setText(lastValues.getProperty("sandCost", "3.00"));
                aggregateCostField.setText(lastValues.getProperty("aggregateCost", "4.00"));
            } catch (IOException ignored) {}
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ConcreteEstimatorGUI::new);
    }
}
