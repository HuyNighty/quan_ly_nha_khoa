package com.manage.nhakhoa.desktop;

import com.manage.nhakhoa.desktop.data.DashboardDataProvider;
import com.manage.nhakhoa.desktop.model.DashboardViewModel;
import com.manage.nhakhoa.desktop.ui.Theme;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.SwingWorker;

/** Chỉ kết quả của lần tải mới nhất được phép render; listener luôn chạy trên EDT. */
public final class DashboardLoadController implements AutoCloseable {
    public enum Status { LOADING, LOADED, EMPTY, ERROR }
    public record LoadState(Status status, Optional<DashboardViewModel> data) { }
    private final Clock clock;
    private final Consumer<LoadState> listener;
    private SwingWorker<Optional<DashboardViewModel>, Void> worker;
    private long generation;
    private boolean closed;
    public DashboardLoadController(Clock clock, Consumer<LoadState> listener) {
        this.clock = Objects.requireNonNull(clock); this.listener = Objects.requireNonNull(listener);
    }
    public void reload(DashboardDataProvider provider) {
        Theme.requireEdt(); Objects.requireNonNull(provider);
        if (closed) return;
        long request = ++generation;
        if (worker != null) worker.cancel(true);
        listener.accept(new LoadState(Status.LOADING, Optional.empty()));
        worker = new SwingWorker<>() {
            @Override protected Optional<DashboardViewModel> doInBackground() throws Exception {
                return Objects.requireNonNull(provider.load(clock));
            }
            @Override protected void done() {
                if (closed || request != generation || isCancelled()) return;
                try {
                    Optional<DashboardViewModel> result = get();
                    listener.accept(new LoadState(result.isPresent() ? Status.LOADED : Status.EMPTY, result));
                } catch (CancellationException ignored) {
                    // Một yêu cầu mới hoặc đóng cửa sổ đã thay thế yêu cầu này.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    listener.accept(new LoadState(Status.ERROR, Optional.empty()));
                } catch (ExecutionException e) {
                    listener.accept(new LoadState(Status.ERROR, Optional.empty()));
                }
            }
        };
        worker.execute();
    }
    @Override public void close() {
        Theme.requireEdt(); closed = true; generation++;
        if (worker != null) worker.cancel(true);
    }
}
