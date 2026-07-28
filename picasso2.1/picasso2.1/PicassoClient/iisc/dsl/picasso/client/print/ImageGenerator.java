
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

// Standard imports
import java.awt.image.*;
import java.util.Hashtable;

// Application specific imports
// none

public class ImageGenerator implements ImageConsumer
{
    private Object holder;

    private ColorModel colorModel;
    private WritableRaster raster;
    private int width;
    private int height;

    private BufferedImage image;
    private int[] intBuffer;
    private boolean loadComplete;

    public ImageGenerator()
    {
        holder = new Object();
        width = -1;
        height = -1;
        loadComplete = false;
    }

    public void imageComplete(int status)
    {
            synchronized(holder) {
                loadComplete = true;
                holder.notify();
            }
    }

    public void setColorModel(ColorModel model)
    {
        colorModel = model;
        createImage();
    }

    public void setDimensions(int w, int h)
    {
        width = w;
        height = h;
        createImage();
    }

    public void setHints(int flags)
    {
    }

    public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int offset, int scansize)
    {
        if((intBuffer == null) || (pixels.length > intBuffer.length))
            intBuffer = new int[pixels.length];

        for(int i = pixels.length; --i >= 0 ; )
            intBuffer[i] = (int)pixels[i] & 0xFF;

        raster.setPixels(x, y, w, h, intBuffer);
    }

    public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int offset, int scansize)
    {
        image.setRGB(x, y, w, h, pixels, offset, scansize);
    }

    public void setProperties(Hashtable props)
    {
        createImage();
    }


    public BufferedImage getImage()
    {
        if(!loadComplete) {
            synchronized(holder) {
                try {
                    holder.wait();
                }
                catch(InterruptedException ie) {
                }
            }
        }
        return image;
    }

    private void createImage()
    {
        // meet the preconditions first.
        if((image != null) || (width == -1) || (colorModel == null))
            return;

        // raster = colorModel.createCompatibleWritableRaster(width, height);
        // boolean premult = colorModel.isAlphaPremultiplied();
        // image = new BufferedImage(colorModel, raster, premult, properties);
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }
}
