@artifact.package@import griffon.swing.BindUtils;
import griffon.util.CallableWithArgs;
import griffon.util.RunnableWithArgs;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;

import static griffon.swing.SwingAction.action;

public class PreferencesView extends AbstractDialogView {
    protected JComponent buildContent() {
        Action closeAction = action(message("application.action.Close.name", "Close"))
                .withMnemonic(message("application.action.Close.mnemonic", "C"))
                .withShortDescription(message("application.action.Close.name", "Close"))
                .withRunnable(new RunnableWithArgs() {
                    public void run(Object[] args) {
                        controller.hide();
                    }
                }).build();


        MigLayout layout = new MigLayout();
        layout.setLayoutConstraints("fill");
        JPanel panel = new JPanel(layout);

        JPanel content = new JPanel();
        panel.add(content, "grow, wrap");
        panel.add(new JButton(closeAction), "right");

        String actionKey = "CloseAction";
        panel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"), actionKey);
        panel.getActionMap().put(actionKey, closeAction);

        return panel;
    }
}
