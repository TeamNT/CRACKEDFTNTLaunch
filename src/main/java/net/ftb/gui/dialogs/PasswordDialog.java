/*
 * This file is part of FTB Launcher.
 *
 * Copyright © 2012-2014, FTB Launcher Contributors <https://github.com/Slowpoke101/FTBLaunch/>
 * FTB Launcher is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ftb.gui.dialogs;

import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JRootPane;
import net.ftb.gui.LaunchFrame;
import net.ftb.locale.I18N;

public class PasswordDialog
  extends JDialog
{
  private JLabel passwordLbl;
  private JPasswordField password;
  private JButton login;
  
  public PasswordDialog(LaunchFrame instance, boolean modal)
  {
    super(instance, modal);
    setupGui();
    
    getRootPane().setDefaultButton(this.login);
    this.login.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        if (!new String(PasswordDialog.this.password.getPassword()).isEmpty())
        {
          LaunchFrame.tempPass = new String(PasswordDialog.this.password.getPassword());
          PasswordDialog.this.setVisible(false);
        }
      }
    });
  }
  
  private void setupGui()
  {
    setIconImage(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/image/logo_ftb.png")));
    setTitle(I18N.getLocaleString("PASSWORD_TITLE"));
    setResizable(true);
    
    Container panel = getContentPane();
    
    this.passwordLbl = new JLabel(I18N.getLocaleString("PASSWORD_PASSLABEL"));
    this.password = new JPasswordField(16);
    this.login = new JButton(I18N.getLocaleString("MAIN_SUBMIT"));
    
    panel.add(this.passwordLbl);
    panel.add(this.password, "wrap");
    panel.add(this.login, "center, wrap, span");
    
    pack();
    setLocationRelativeTo(getOwner());
  }
}