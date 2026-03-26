package com.ithuangma.java.ai.langchain4j.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ithuangma.java.ai.langchain4j.entity.Appointment;

public interface AppointmentService extends IService<Appointment> {
    Appointment getOne(Appointment appointment);
}
