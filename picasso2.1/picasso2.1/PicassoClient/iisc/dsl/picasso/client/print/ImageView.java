
/*
 # 
 # 
 # PROGRAM INFORMATION
 # 
 # 
 # Copyright (C) 2006 Indian Institute of Science, Bangalore, India.
 # All rights reserved.
 # 
 # This program is part of the Picasso Database Query Optimizer Visualizer
 # software distribution invented at the Database Systems Lab, Indian
 # Institute of Science (PI: Prof. Jayant R. Haritsa). The software is
 # free and its use is governed by the licensing agreement set up between
 # the copyright owner, Indian Institute of Science, and the licensee.
 # The software is distributed without any warranty; without even the
 # implied warranty of merchantability or fitness for a particular purpose.
 # The software includes external code modules, whose use is governed by
 # their own licensing conditions, which can be found in the Licenses file
 # of the Docs directory of the distribution.
 # 
 # 
 # The official project web-site is
 #     http://dsl.serc.iisc.ernet.in/projects/PICASSO/picasso.html
 # and the email contact address is 
 #     picasso@dsl.serc.iisc.ernet.in
 # 
 #
*/

package iisc.dsl.picasso.client.print;

import iisc.dsl.picasso.client.util.PicassoUtil;
import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*;

public class ImageView extends FileView {
	ImageIcon jpgIcon = PicassoUtil.createImageIcon("images/jpgIcon.gif");
	
    public String getName(File f) {
        return null;
    }

    public String getDescription(File f) {
        return null;
    }

    public Boolean isTraversable(File f) {
        return null;
    }

    public String getTypeDescription(File f) {
        String extension = PicassoUtil.getExtension(f.getName());
        String type = null;

        if (extension != null) {
            if (extension.equals("jpeg") ||
                extension.equals("jpg")) {
                type = "JPEG Image";
            }
            else if (extension.equals("png")) {
            	type = "PNG Image";
            }
        }
        return type;
    }

    public Icon getIcon(File f) {
        String extension = PicassoUtil.getExtension(f.getName());
        Icon icon = null;

        if (extension != null) {
            if (extension.equals("jpeg") ||
                extension.equals("jpg")) {
                icon = jpgIcon;
            }
        }
        return icon;
    }
}
