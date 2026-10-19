package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.Appointment;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.ExamTicket;
import com.manage.nhakhoa.desktop.demo.DemoClinicStore.Patient;
import com.manage.nhakhoa.desktop.ui.CardList;
import com.manage.nhakhoa.desktop.ui.DemoTable.Tag;
import com.manage.nhakhoa.desktop.ui.Flat;
import com.manage.nhakhoa.desktop.ui.Theme;
import java.awt.FontMetrics;
import java.time.Period;
import java.util.function.Function;
import static com.manage.nhakhoa.desktop.ui.DemoWidgets.*;

/** Cách vẽ dòng thẻ dùng chung: bệnh nhân (avatar tròn) và lịch hẹn (khối giờ). */
final class Rows {
    private Rows() { }
    static final int PATIENT_HEIGHT = 66, APPOINTMENT_HEIGHT = 62;

    static String age(DemoClinicStore store, Patient p) { return Period.between(p.birthDate(), store.today()).getYears() + " tuổi"; }

    /** Dòng bệnh nhân: avatar, họ tên, mã · giới tính · tuổi · SĐT che, viên trạng thái bên phải (tùy chọn). */
    static CardList<Patient> patients(String name, DemoClinicStore store, Function<Patient, Tag> badge, int maxRows) {
        return new CardList<>(name, Patient::id, (g, p, r, selected, hover) -> {
            int avatar = 42, x = 18, cy = r.height / 2;
            Flat.paintAvatar(g, x, cy - avatar / 2, avatar, p.name(), Flat.avatarTone(p.id()));
            int textX = x + avatar + 14, right = r.width - 16, nameRight = right;
            Tag tag = badge == null ? null : badge.apply(p);
            // Viên trạng thái nằm cùng dòng tên; dòng mô tả dùng trọn bề ngang để không bị cắt.
            if (tag != null) nameRight = CardList.pillRight(g, tag.text(), right, cy - 9, tag.color()) - 10;
            CardList.text(g, p.name(), textX, cy - 4, nameRight - textX, Theme.font(14, true), Theme.TEXT);
            CardList.text(g, p.id() + "  ·  " + p.gender().label() + "  ·  " + age(store, p) + "  ·  " + DemoClinicStore.maskPhone(p.phone()),
                    textX, cy + 16, right - textX, Theme.font(12, false), Theme.MUTED);
        }, PATIENT_HEIGHT, maxRows);
    }

    /** Dòng lịch hẹn: khối giờ + mã bên trái, bác sĩ + lý do ở giữa, trạng thái + phiếu bên phải. */
    static CardList<Appointment> appointments(String name, DemoClinicStore store, int maxRows) {
        return new CardList<>(name, Appointment::id, (g, a, r, selected, hover) -> {
            int cy = r.height / 2;
            g.setColor(selected ? Flat.tint(Theme.PRIMARY, 0.8) : Theme.SOFT); g.fillRoundRect(16, 9, 64, r.height - 18, 10, 10);
            g.setFont(Theme.font(16, true)); FontMetrics fm = g.getFontMetrics(); String time = TIME.format(a.time());
            g.setColor(Theme.TEXT); g.drawString(time, 16 + (64 - fm.stringWidth(time)) / 2, cy + 6);
            int textX = 96, right = r.width - 16;
            right = CardList.pillRight(g, a.status().label(), right, cy - 8, tone(a.status())) - 10;
            String ticket = store.ticketForAppointment(a.id()).map(ExamTicket::id).map(id -> "Phiếu " + id).orElse(a.id());
            g.setFont(Theme.font(11, false)); FontMetrics small = g.getFontMetrics();
            CardList.text(g, ticket, r.width - 16 - small.stringWidth(ticket), cy + 16, 140, Theme.font(11, false), Theme.MUTED);
            CardList.text(g, store.doctorName(a.doctorId()), textX, cy - 3, right - textX, Theme.font(13, true), Theme.TEXT);
            CardList.text(g, a.reason(), textX, cy + 16, r.width - 170 - textX, Theme.font(12, false), Theme.MUTED);
        }, APPOINTMENT_HEIGHT, maxRows);
    }
}
