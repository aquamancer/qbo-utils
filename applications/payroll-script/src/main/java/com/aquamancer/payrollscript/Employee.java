package com.aquamancer.payrollscript;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Employee {
    Map<String, String> data;
    boolean inputted;
    public static List<Employee> getEmployeeList(List<Map<String, String>> data) {
        List<Employee> employees = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            employees.add(new Employee(data.get(i)));
        }
        return employees;
    }
    public Employee(Map<String, String> data) {
        this.data = data;
        inputted = false;
    }
    public void printInfo() {
        for (String key : data.keySet()) {
            System.out.print(key + ": " + data.get(key) + "\t");
        }
        System.out.println();
    }
}
