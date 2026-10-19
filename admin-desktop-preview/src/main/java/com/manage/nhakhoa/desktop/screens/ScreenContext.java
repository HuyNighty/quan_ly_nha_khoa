package com.manage.nhakhoa.desktop.screens;

import com.manage.nhakhoa.desktop.demo.DemoClinicStore;
import com.manage.nhakhoa.desktop.ui.DemoDialogs;
import com.manage.nhakhoa.desktop.ui.UiComponents;
import java.awt.Component;
import java.util.Objects;
import javax.swing.JButton;
import java.util.function.Consumer;

/** Phụ thuộc chung của các màn mô phỏng: kho demo, điều hướng theo khóa MenuDefinition, hộp thoại xác nhận. */
public record ScreenContext(DemoClinicStore store, Consumer<String> navigate, DemoDialogs dialogs) {
    public ScreenContext {
        Objects.requireNonNull(store); Objects.requireNonNull(navigate); Objects.requireNonNull(dialogs);
    }
    /** Nút khôi phục bộ mẫu ban đầu cho toàn bộ màn mô phỏng (có xác nhận). */
    public JButton resetButton(Component parent, String name) {
        JButton b = UiComponents.secondary("Khôi phục dữ liệu mẫu", () -> {
            if (dialogs.confirm(parent, "Khôi phục dữ liệu mẫu", "Bỏ mọi thay đổi mô phỏng của phiên và nạp lại bộ mẫu ban đầu?")) store.reset();
        });
        b.setName(name); b.setToolTipText("Nạp lại seed deterministic; không ảnh hưởng hệ thống thật"); return b;
    }
}
