/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Other/File.java to edit this template
 */

/**
 *
 * @author Joni
 */
public class Student {
    private String studentName;
    private String number;
    
    public Student(String name, String studentNumber){
        studentName = name;
        number = studentNumber;
    }
    public String getName(){
        return studentName;
    }
    public String getStudentNumber(){
        return number;
    }
}
