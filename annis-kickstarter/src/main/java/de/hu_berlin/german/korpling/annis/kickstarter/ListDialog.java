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
import annis.administration.ImportStatus;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author thomas
 */
public class ListDialog extends javax.swing.JDialog
{

  private DefaultTableModel tableModel;
  private CorpusAdministration corpusAdmin;

  /** Creates new form ListDialog */
  public ListDialog(java.awt.Frame parent, boolean modal, CorpusAdministration corpusAdmin)
  {
    super(parent, modal);

    this.corpusAdmin = corpusAdmin;

    initComponents();

    updateTable();
  }

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        tableList = new javax.swing.JTable();
        btClose = new javax.swing.JButton();
        btDelete = new javax.swing.JButton();
        pbDelete = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("List corpora - ANNIS Kickstarter");
        setLocationByPlatform(true);

        tableList.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane1.setViewportView(tableList);

        btClose.setMnemonic('c');
        btClose.setText("Close");
        btClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btCloseActionPerformed(evt);
            }
        });

        btDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/hu_berlin/german/korpling/annis/kickstarter/crystal_icons/edit_remove.png"))); // NOI18N
        btDelete.setText("Delete selected corpus");
        btDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btDeleteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 627, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(btDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 188, Short.MAX_VALUE)
                        .addGap(368, 368, 368)
                        .addComponent(btClose)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pbDelete, javax.swing.GroupLayout.DEFAULT_SIZE, 188, Short.MAX_VALUE)
                        .addGap(427, 427, 427))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 309, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7)
                .addComponent(pbDelete, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btDelete)
                    .addComponent(btClose, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btCloseActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btCloseActionPerformed
    {//GEN-HEADEREND:event_btCloseActionPerformed

      setVisible(false);
    }//GEN-LAST:event_btCloseActionPerformed

    private void btDeleteActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btDeleteActionPerformed
    {//GEN-HEADEREND:event_btDeleteActionPerformed

      int row = tableList.getSelectedRow();
      int col = tableModel.findColumn("id");

      if(row > -1 && col > -1)
      {
        Object value = tableModel.getValueAt(row, col);
        final LinkedList<Long> corpusListToDelete = new LinkedList<Long>();
        long l = Long.parseLong(value.toString());
        corpusListToDelete.add(l);

        pbDelete.setIndeterminate(true);
        btClose.setEnabled(false);
        btDelete.setEnabled(false);

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>()
        {

          @Override
          protected String doInBackground() throws Exception
          {
            corpusAdmin.deleteCorpora(corpusListToDelete);
            updateTable();
            return "";
          }

          @Override
          protected void done()
          {
            pbDelete.setIndeterminate(false);
            pbDelete.setValue(100);
            btClose.setEnabled(true);
            btDelete.setEnabled(true);
          }
        };

        worker.execute();

      }

    }//GEN-LAST:event_btDeleteActionPerformed

  private void updateTable()
  {
    try
    {
      tableModel = new DefaultTableModel(new String[]
        {
          "name", "id", "text", "tokens", "source_path"
        }, 0);
      tableList.setModel(tableModel);

      List<Map<String, Object>> stats = corpusAdmin.listCorpusStats();

      int row = 0;
      for(Map<String, Object> map : stats)
      {
        String[] rowData = new String[tableModel.getColumnCount()];
        for(int j = 0; j < rowData.length; j++)
        {
          String cName = tableList.getColumnName(j);
          if(map.containsKey(cName))
          {
            rowData[j] = map.get(cName).toString();
          }
          else
          {
            rowData[j] = "";
          }
        }

        tableModel.addRow(rowData);
        row++;
      }
    }
    catch(Exception ex)
    {
      ImportStatus importStatus = corpusAdmin.getAdministrationDao().initImportStatus();
      importStatus.addException("list corpora exception", ex);
      new ExceptionDialog(importStatus).setVisible(true);
    }
  }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btClose;
    private javax.swing.JButton btDelete;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JProgressBar pbDelete;
    private javax.swing.JTable tableList;
    // End of variables declaration//GEN-END:variables
}