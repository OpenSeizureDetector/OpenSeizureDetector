#!/usr/bin/python
#
#############################################################################
#
# Copyright Graham Jones, June 2014 
#
#############################################################################
#
#   This file is part of OpenSeizureDetector.
#
#    OpenSeizureDetector is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    Foobar is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
#
#    You should have received a copy of the GNU General Public License
#    along with OpenSeizureDetector.  If not, see <http://www.gnu.org/licenses/>.
##############################################################################
#
import pygtk
pygtk.require('2.0')
import gtk
import cv2
import os

class GtkTrainer:
    curFrame = 0
    selArea = [0,0,50,50]
    dragging = False
    def getFrame(self,deltaFrameNo):
        print deltaFrameNo
        self.curFrame = self.curFrame + deltaFrameNo
        if (self.curFrame<0):
            self.curFrame = 0
        if (self.curFrame>=self.capture.get(cv2.cv.CV_CAP_PROP_FRAME_COUNT)):
            self.curFrame = self.capture.get(cv2.cv.CV_CAP_PROP_FRAME_COUNT)
        print self.curFrame
        self.capture.set(cv2.cv.CV_CAP_PROP_POS_FRAMES,
                         self.curFrame)
        retval,self.frame = self.capture.read(0)
        print retval
        return self.frame

    def drawImage(self):
        self.cm = self.da.window.get_colormap()
        self.gc = self.da.window.new_gc(foreground=self.cm.alloc_color('#ff0000',True,False),
                                        function=gtk.gdk.COPY)
        x,y,width,height = self.da.get_allocation()
        drawable = self.da.window
        rgbFrame = cv2.cvtColor(self.frame,cv2.COLOR_BGR2RGB)
        rgbFrame = cv2.resize(rgbFrame,(width,height))
        img_pixbuf = gtk.gdk.pixbuf_new_from_data(rgbFrame.tostring(),
                                                  gtk.gdk.COLORSPACE_RGB,
                                                  False,
                                                  8,
                                                  rgbFrame.shape[1],
                                                  rgbFrame.shape[0],
                                                  rgbFrame.shape[1]*3)
        pixmap,mask = img_pixbuf.render_pixmap_and_mask()
        pixmap.draw_rectangle(self.gc,
                                      False,
                                      self.selArea[0],self.selArea[1],
                                      self.selArea[2],self.selArea[3])
        #self.da.set_from_pixmap(pixmap,mask)
        self.da.window.draw_drawable(self.gc,
                                     pixmap,
                                     x,y,x,y,width,height)
        self.da.show()


    def changeFrame(self,widget,data=0):
        print "getFrame(%s)" % data
        self.getFrame(data)
        self.drawImage()

    def delete_event(self,widget,event,data=None):
        print "delete event"
        return False

    def destroy(self,widget,data=None):
        gtk.main_quit()

    def configure_event(self,widget,event):
        print "configure_event"
        return gtk.TRUE

    def expose_event(self,widget,event):
        print "expose_event"
        #self.drawImage()
        return gtk.FALSE

    def motion_event(self,widget,event):
        '''Handle dragging the mouse over the image to mark the area
        that contains the target object'''
        if event.is_hint:
            #print "hint"
            x,y,state = event.window.get_pointer()
        else:
            #print "motion event"
            x = event.x
            y = event.y
            state = event.state
        # Is the mouse button pressed?
        if state & gtk.gdk.BUTTON1_MASK:
            # if we are already dragging, update the 
            # bottom right corner of the selection area.
            if self.dragging:
                #print x,y
                self.selArea[2] = x-self.selArea[0]
                self.selArea[3] = y-self.selArea[1]
                self.drawImage()
            # Otherwise set the top left corner.
            else:
                #print "drag start"
                self.selArea[0]=x
                self.selArea[1]=y
                self.dragging = True
        # if the button is not pressed, we have finnished dragging.
        else:
            if self.dragging:
                self.dragging = False
                #print "drag end"
        return True

    def savePos(self,widget,event):
        target = self.targetText.get_text()
        print "savePos",target
        os.makedirs(target)
        

    def saveNeg(self,widget,event):
        print "saveNeg"

    def button_event(self,widget,event):
        print "button event"
        return True

    def __init__(self,fname):
        self.capture = cv2.VideoCapture(fname)
        self.frame = self.getFrame(0)
        self.window = gtk.Window(gtk.WINDOW_TOPLEVEL)
        self.window.set_border_width(10)
        self.vbox =gtk.VBox(False,0)
        self.box1 =gtk.HBox(False,0)
        self.box2 =gtk.HBox(False,0)
        self.da = gtk.DrawingArea()
        self.da.set_size_request(640,480)
        self.da.set_events(gtk.gdk.EXPOSURE_MASK |
                           gtk.gdk.LEAVE_NOTIFY_MASK | 
                           gtk.gdk.BUTTON_PRESS_MASK |
                           gtk.gdk.POINTER_MOTION_MASK |
                           gtk.gdk.POINTER_MOTION_HINT_MASK)
        self.f1b = gtk.Button(">")
        self.f10b = gtk.Button(">>")
        self.b1b = gtk.Button("<")
        self.b10b = gtk.Button("<<")
        self.savePosButton = gtk.Button("Save as Positive")
        self.saveNegButton = gtk.Button("Save as Negative")
        self.targetText = gtk.Entry()
        self.window.connect("delete_event",self.delete_event)
        self.window.connect("destroy",self.destroy)
        self.da.connect("expose_event",self.expose_event)
        self.da.connect("configure_event",self.configure_event)
        self.da.connect("motion_notify_event",self.motion_event)
        self.da.connect("button_press_event",self.button_event)
        self.f1b.connect("clicked",self.changeFrame,1)
        self.f10b.connect("clicked",self.changeFrame,10)
        self.b1b.connect("clicked",self.changeFrame,-1)
        self.b10b.connect("clicked",self.changeFrame,-10)
        self.savePosButton.connect("clicked",self.savePos,0)
        self.saveNegButton.connect("clicked",self.saveNeg,0)
        self.da.show()
        self.f1b.show()
        self.f10b.show()
        self.b1b.show()
        self.b10b.show()
        self.savePosButton.show()
        self.saveNegButton.show()
        self.targetText.show()
        self.vbox.pack_start(self.da,True,True,0)
        self.vbox.pack_start(self.box1,True,True,0)
        self.vbox.pack_start(self.box2,True,True,0)
        self.box1.pack_start(self.b10b,True,True,0)
        self.box1.pack_start(self.b1b,True,True,0)
        self.box1.pack_start(self.f1b,True,True,0)
        self.box1.pack_start(self.f10b,True,True,0)
        self.box2.pack_start(self.savePosButton,True,True,0)
        self.box2.pack_start(self.saveNegButton,True,True,0)
        self.box2.pack_start(self.targetText,True,True,0)
        self.window.add(self.vbox)
        self.vbox.show()
        self.box1.show()
        self.box2.show()
        self.window.show()


    def main(self):
        gtk.main()

if __name__ == "__main__":
    trainer = GtkTrainer("hand_test.mp4")
    trainer.main()
