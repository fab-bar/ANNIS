/*
 * Copyright 2009-2011 Collaborative Research Centre SFB 632 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.hu_berlin.german.korpling.annis.kickstarter;

import annis.administration.CorpusAdministration;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 *
 * @author thomas
 */
public class InitDialog extends javax.swing.JDialog
{

  private CorpusAdministration corpusAdministration;
  private SwingWorker<String, Void> initWorker;

  /** Creates new form InitDialog */
  public InitDialog(java.awt.Frame parent, boolean modal, final CorpusAdministration corpusAdministration)
  {
    super(parent, modal);
    initComponents();

    this.corpusAdministration = corpusAdministration;

    final InitDialog finalThis = this;

    initWorker = new SwingWorker<String, Void>()
    {

      @Override
      protected String doInBackground() throws Exception
      {
        try
        {
          corpusAdministration.initializeDatabase("localhost", "5432", "anniskickstart",
            "anniskickstart", "annisKickstartPassword", "postgres",
            txtAdminUsername.getText(), new String(txtAdminPassword.getPassword()));

          return "";
        } catch (Exception ex)
        {
          finalThis.setVisible(false);
          ExceptionDialog dlg = new ExceptionDialog(finalThis, ex);
          dlg.setVisible(true);
        }

        return "ERROR";
      }

      @Override
      protected void done()
      {
        pbInit.setIndeterminate(false);
        btOk.setEnabled(true);
        btCancel.setEnabled(true);
        try
        {
          if ("".equals(this.get()))
          {
            pbInit.setValue(100);
            JOptionPane.showMessageDialog(null, "Database initialized.", "INFO",
              JOptionPane.INFORMATION_MESSAGE);
            setVisible(false);
          }
          else
          {
            pbInit.setValue(0);
          }
        }
        catch (InterruptedException ex)
        {
          Logger.getLogger(InitDialog.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex)
        {
          Logger.getLogger(InitDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    };

  }

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jLabel1 = new javax.swing.JLabel();
    jLabel2 = new javax.swing.JLabel();
    btOk = new javax.swing.JButton();
    btCancel = new javax.swing.JButton();
    txtAdminUsername = new javax.swing.JTextField();
    txtAdminPassword = new javax.swing.JPasswordField();
    pbInit = new javax.swing.JProgressBar();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("Init - Annis² Kickstarter");
    setLocationByPlatform(true);

    jLabel1.setText("Postgres-Admin username:");

    jLabel2.setText("Postgres-Admin password:");

    btOk.setMnemonic('o');
    btOk.setText("Ok");
    btOk.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btOkActionPerformed(evt);
      }
    });

    btCancel.setMnemonic('c');
    btCancel.setText("Cancel");
    btCancel.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btCancelActionPerformed(evt);
      }
    });

    txtAdminUsername.setText("postgres");

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(pbInit, javax.swing.GroupLayout.DEFAULT_SIZE, 396, Short.MAX_VALUE)
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel1)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(txtAdminUsername, javax.swing.GroupLayout.DEFAULT_SIZE, 221, Short.MAX_VALUE))
          .addGroup(layout.createSequentialGroup()
            .addComponent(jLabel2)
            .addGap(18, 18, 18)
            .addComponent(txtAdminPassword, javax.swing.GroupLayout.DEFAULT_SIZE, 212, Short.MAX_VALUE))
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addComponent(btCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 186, Short.MAX_VALUE)
            .addComponent(btOk, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel1)
          .addComponent(txtAdminUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(jLabel2)
          .addComponent(txtAdminPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addComponent(pbInit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(btOk)
          .addComponent(btCancel))
        .addContainerGap())
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

    private void btCancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btCancelActionPerformed
    {//GEN-HEADEREND:event_btCancelActionPerformed

      setVisible(false);

    }//GEN-LAST:event_btCancelActionPerformed

    private void btOkActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btOkActionPerformed
    {//GEN-HEADEREND:event_btOkActionPerformed
      
      pbInit.setIndeterminate(true);
      btOk.setEnabled(false);
      btCancel.setEnabled(false);
      initWorker.execute();

    }//GEN-LAST:event_btOkActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton btCancel;
  private javax.swing.JButton btOk;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JProgressBar pbInit;
  private javax.swing.JPasswordField txtAdminPassword;
  private javax.swing.JTextField txtAdminUsername;
  // End of variables declaration//GEN-END:variables
}
