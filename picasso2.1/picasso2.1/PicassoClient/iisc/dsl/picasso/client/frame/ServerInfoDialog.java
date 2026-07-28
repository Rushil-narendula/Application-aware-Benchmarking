package iisc.dsl.picasso.client.frame;

import iisc.dsl.picasso.client.panel.MainPanel;
import iisc.dsl.picasso.common.PicassoConstants;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;

public class ServerInfoDialog extends JDialog implements ActionListener {
	
	private static final long serialVersionUID = -8088437034960288910L;
	
	private JTextField 	snameText, portText;
	private JButton		okButton, cancelButton;
	private MainPanel	mainPanel;
	String serverName;
	int	port;
	boolean firstTime = true;
	
	public ServerInfoDialog(MainPanel mp) {
		super(mp.getFrame(), "Connect To Picasso Server", true);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			System.out.println(e);
		}
		
		mainPanel = mp;
		
		createGUI(mp.getServerName(), mp.getServerPort());
		snameText.addFocusListener(focusListener);
		portText.addFocusListener(focusListener);
		okButton.addActionListener(this);
		cancelButton.addActionListener(this);
	}
	
	private void createGUI(String sName, int sPort) {
		if (firstTime == true) 
			mainPanel.setPanel();
		
		System.out.println("Connect to PServer");
		setTitle("Enter Picasso Server Info");
		setSize(270, 200);
		setLocation(400, 350);
		GridBagLayout gb = new GridBagLayout();
		getContentPane().setLayout(gb);
		GridBagConstraints c = new GridBagConstraints();
		
		if ( sName.length() == 0 )
			serverName = "localhost";
		else serverName = sName;
		
		if ( sPort == 0 )
			port = PicassoConstants.SERVER_PORT;
		else port = sPort;
		
		snameText = new JTextField(serverName);
		portText = new JTextField(""+port);
		
		Font f = new Font("Courier", Font.BOLD, 12);
		JLabel portLbl = new JLabel("Port: ", JLabel.RIGHT);
		portLbl.setFont(f);		

		JLabel srvrLbl = new JLabel("Machine: ", JLabel.RIGHT);
		srvrLbl.setFont(f);
		
		okButton = new JButton("OK");
		cancelButton = new JButton("Cancel");
		
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		//c.ipady = 2;
		c.insets = new Insets(10, 20, 0 , 20);
		c.fill = GridBagConstraints.HORIZONTAL;
		
		c.gridx = 0;
		c.gridy = 0;
		getContentPane().add(srvrLbl, c);
		
		c.gridx = 1;
		c.gridy = 0;
		getContentPane().add(snameText, c);
		
		c.gridx = 0;
		c.gridy = 1;
		getContentPane().add(portLbl, c);
		
		c.gridx = 1;
		c.gridy = 1;
		getContentPane().add(portText, c);
		
		
		c.gridx = 0;
		c.gridy = 2;
		getContentPane().add(okButton, c);
		
		c.gridx = 1;
		c.gridy = 2;
		getContentPane().add(cancelButton, c);
	}

	
	public void actionPerformed(ActionEvent e) {
		this.serverName = snameText.getText();
		String port = portText.getText();
		if (port == "" )
			this.port = 0;
		this.port = Integer.parseInt(port);
		
		if ( e.getSource() == okButton ) {			
			if (this.serverName.length() == 0 )
				this.serverName = "";
			mainPanel.setServerName(this.serverName);
			mainPanel.setPort(this.port);
			this.dispose();
			mainPanel.sendInitialMessageToServer();
			firstTime = false;
			
		} else if ( e.getSource() == cancelButton ) {
			this.dispose();
			if ( firstTime ) {
				mainPanel.setServerName("");
				mainPanel.setPort(0);
			}
			firstTime = false;
		}
	}
	
	FocusListener focusListener = new FocusListener() {

		public void focusGained(FocusEvent e) {

		}

		public void focusLost(FocusEvent e) {
		}
	};
}
