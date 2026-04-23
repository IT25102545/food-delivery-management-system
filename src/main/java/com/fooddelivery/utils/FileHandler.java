package com.fooddelivery.utils;

import com.fooddelivery.dao.OrderDAO;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;

public class FileHandler {

    public static String generateDailyReport() {
        OrderDAO orderDAO = new OrderDAO();
        double totalRevenue = orderDAO.getTotalRevenue();
        int totalOrders = orderDAO.getTotalOrders();
        
        String reportDate = LocalDate.now().toString();
        String reportContent = "--- Daily Revenue Report ---\n" +
                               "Date: " + reportDate + "\n" +
                               "Total Orders: " + totalOrders + "\n" +
                               "Total Revenue: $" + String.format("%.2f", totalRevenue) + "\n" +
                               "----------------------------\n";
        
        String filePath = "daily_report.txt";
        
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(reportContent);
            return "Report generated successfully at " + filePath;
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to generate report: " + e.getMessage();
        }
    }
}
