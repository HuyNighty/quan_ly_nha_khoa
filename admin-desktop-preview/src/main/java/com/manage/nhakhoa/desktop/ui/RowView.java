package com.manage.nhakhoa.desktop.ui;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Danh sách có thể chọn theo mã (bảng hoặc danh sách thẻ), dùng chung cho màn hình và kiểm thử. */
public interface RowView<T> {
    List<T> rows();
    Optional<T> selected();
    /** Chọn theo mã và phát sự kiện chọn; false nếu không có trong danh sách. */
    boolean select(String id);
    void clearSelection();
    void setRows(List<T> rows, String emptyText);
    void onSelect(Consumer<T> listener);
    boolean isEmptyState();
    String emptyText();
}
