import com.fazecast.jSerialComm.SerialPort;
import org.apache.commons.lang3.time.StopWatch;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Scanner;

/**
 * Created by Armand on 3/11/17.
 */
public class Main {
    public static SerialPort[] ports;
    public static StopWatch watch = new StopWatch();
    private static JFrame frame = new JFrame();
    private static JPanel top = new JPanel();
    private static JPanel mid = new JPanel();
    private static JPanel bottom = new JPanel();
    private static JLabel labelVal = new JLabel("0");
    private static JSlider sliderVal = new JSlider();
    private static JButton commConnect = new JButton("Connect");
    private static JButton commRefresh = new JButton("Refresh");
    private static JComboBox<String> portList = new JComboBox<>();
    private static XYSeries graph = new XYSeries("Potentiometer Values");
    private static XYSeriesCollection dataset = new XYSeriesCollection(graph);
    private static SerialPort chosenPort;
    private static JFreeChart chart = ChartFactory.createXYLineChart("Potentiometer Values Over Time", "Time (seconds)", "Potentiometer Values", dataset, PlotOrientation.VERTICAL, false, false, false);
    private static XYPlot xyPlot = chart.getXYPlot();
    private static ValueAxis domainAxis = xyPlot.getDomainAxis();
    private static ValueAxis rangeAxis = xyPlot.getRangeAxis();
    private static Color trans = new Color(255, 255, 255, 0);

    public static void main(String[] args) {
        gui();

        ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            portList.addItem(port.getSystemPortName());
        }

        commConnect.addActionListener(new ActionListener(){
            @Override public void actionPerformed(ActionEvent arg0) {
                if(commConnect.getText().equals("Connect")) {
                    chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
                    chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
                    if(chosenPort.openPort()) {
                        commConnect.setText("Disconnect");
                        portList.setEnabled(false);
                        commRefresh.setEnabled(false);
                    }

                    Thread thread = new Thread(){
                        @Override public void run() {
                            Scanner scanner = new Scanner(chosenPort.getInputStream());
                            watch.start();
                            domainAxis.setRange(0, 5);
                            while(scanner.hasNextLine()) {
                                try {
                                    String line = scanner.nextLine();
                                    int number = Integer.parseInt(line);
                                    graph.add(watch.getTime() / 1000.0, number);
                                    if (watch.getTime() >= 5000) {
                                        domainAxis.setRange((watch.getTime() - 5000) / 1000.0, watch.getTime() / 1000.0);
                                    }
                                    sliderVal.setValue(number);
                                    labelVal.setText(Integer.toString(number));
                                } catch(Exception e) {}
                            }

                            watch.stop();
                            watch.reset();
                            chosenPort.closePort();
                            portList.setEnabled(true);
                            commRefresh.setEnabled(true);
                            commConnect.setText("Connect");
                            graph.clear();
                            domainAxis.setRange(0.0, 5.0);
                            sliderVal.setValue(0);
                            labelVal.setText("0");

                            scanner.close();
                        }
                    };
                    thread.start();
                } else {
                    watch.stop();
                    watch.reset();
                    chosenPort.closePort();
                    portList.setEnabled(true);
                    commRefresh.setEnabled(true);
                    commConnect.setText("Connect");
                    graph.clear();
                    domainAxis.setRange(0.0, 5.0);
                    sliderVal.setValue(0);
                    labelVal.setText("0");
                }
            }
        });

        commRefresh.addActionListener(new ActionListener(){
            @Override public void actionPerformed(ActionEvent arg0) {
                    portList.removeAllItems();
                    ports = SerialPort.getCommPorts();
                    for (SerialPort port : ports) {
                        portList.addItem(port.getSystemPortName());
                    }
            }
        });
    }

    public static void gui() {
        frame.setTitle("Potentiometer Serial Readings");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700,550);
        frame.setLayout(new BorderLayout());

        top.setLayout(new FlowLayout(FlowLayout.CENTER));
        top.add(commRefresh);
        top.add(portList);
        top.add(commConnect);

        mid.setLayout(new FlowLayout(FlowLayout.CENTER));
        sliderVal.setMaximum(1023);
        mid.add(sliderVal);
        mid.add(labelVal);

        bottom.setLayout(new FlowLayout(FlowLayout.CENTER));
        chart.setBackgroundPaint(trans);
        rangeAxis.setRange(0, 1023);
        bottom.add(new ChartPanel(chart));


        frame.add(top, BorderLayout.NORTH);
        frame.add(mid, BorderLayout.CENTER);
        frame.add(bottom, BorderLayout.SOUTH);
        frame.setVisible(true);
    }
}
