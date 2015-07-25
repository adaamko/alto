/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import com.ezware.dialog.task.TaskDialogs;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 *
 * @author koller
 */
public class GuiUtils {
    private static ProgressListener globalListener = null;
    
    public static void copyToClipboard(String s) {
        StringSelection selection = new StringSelection(s);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    /**
     * Execute this on the EDT.
     *
     * @param <E>
     * @param parent
     * @param title
     * @param description
     * @param worker
     * @param andThen
     */
    public static <E> void withProgressBar(Frame parent, String title, String description, ProgressBarWorker<E> worker, ValueAndTimeConsumer<E> andThen) {
        final ProgressBarDialog progressBar = new ProgressBarDialog(title, description, parent, false);
        final JProgressBar bar = progressBar.getProgressBar();
        progressBar.getProgressBar().setStringPainted(true);
        progressBar.setVisible(true);

        ProgressListener listener = (currentValue, maxValue, string) -> {
            SwingUtilities.invokeLater(() -> {
                bar.setMaximum(maxValue);
                bar.setValue(currentValue);

                if (string == null) {
                    if (maxValue == 0) {
                        bar.setString("");
                    } else {
                        bar.setString(currentValue * 100 / maxValue + "%");
                    }
                } else {
                    bar.setString(string);
                }
            });
        };

        Thread workerThread = new Thread(() -> {
            long start = System.nanoTime();

            try {
                final E result = worker.compute(listener);
                final long time = System.nanoTime() - start;

                SwingUtilities.invokeLater(() -> {
                    andThen.accept(result, time);
                });
            } catch (Exception e) {
                showError(e);
            } finally {
                progressBar.setVisible(false);
            }
        });
        
        workerThread.setName("Alto worker thread: " + description);
        workerThread.start();
    }

    static public void showError(Component parent, String error) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(parent, error, "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    static public void showError(Throwable error) {
        SwingUtilities.invokeLater(() -> {
            TaskDialogs.showException(error);
        });
    }

    public static ProgressListener getGlobalListener() {
        return globalListener;
    }

    /**
     * Sets the global ProgressListener. The preferred method of
     * giving a progress listener to a worker is to pass it as
     * a method argument. This method is only here for cases when
     * this would mess up the code to a terrible extent. You should
     * avoid it whenever possible.
     * 
     * @return 
     */
    public static void setGlobalListener(ProgressListener globalListener) {
        GuiUtils.globalListener = globalListener;
    }
    
    
}
