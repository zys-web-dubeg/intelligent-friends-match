package com.ithuangma.java.ai.langchain4j;

import com.ithuangma.java.ai.langchain4j.Service.AppointmentService;
import com.ithuangma.java.ai.langchain4j.entity.Appointment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AppointmentServiceTest {
    @Autowired
    private AppointmentService appointmentService;
    @Test
    void testGetOne() {
        Appointment appointment = new Appointment();
        appointment.setUsername("张三");
        appointment.setIdCard("123456789012345678");
        appointment.setDepartment("内科");
        appointment.setDate("2026-04-14");
        appointment.setTime("上午");
        Appointment appointmentDB = appointmentService.getOne(appointment);
        System.out.println(appointmentDB != null);
        System.out.println(appointmentDB);
    }
    @Test
    void testSave() {
        Appointment appointment = new Appointment();
        appointment.setUsername("张三");
        appointment.setIdCard("123456789012345678");
        appointment.setDepartment("内科");
        appointment.setDate("2026-04-14");
        appointment.setTime("上午");
        appointment.setDoctorName("王医生");
        appointmentService.save(appointment);
    }
    @Test
    void testRemoveById() {
        appointmentService.removeById(1L);
    }
}
