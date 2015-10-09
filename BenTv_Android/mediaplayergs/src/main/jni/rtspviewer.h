/*
 * Copyright (C) 2014 Ognyan Tonchev <otonchev at gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

/*
 * GstRTSPViewer: GstRTSPStreamer and GstWindowRenderer creating a RTSP
 * pipeline which displays the video content on the screen.
 */
#ifndef _GST_RTSP_VIEWER_H_
#define _GST_RTSP_VIEWER_H_

#include <glib.h>
#include <glib-object.h>

G_BEGIN_DECLS

#define GST_TYPE_RTSP_VIEWER (gst_rtsp_viewer_get_type ())
#define GST_RTSP_VIEWER(object) (G_TYPE_CHECK_INSTANCE_CAST ((object), GST_TYPE_RTSP_VIEWER, GstRTSPViewer))
#define GST_RTSP_VIEWER_CLASS(klass) (G_TYPE_CHECK_CLASS_CAST ((klass), GST_RTSP_VIEWER_TYPE, GstRTSPViewerClass))
#define GST_IS_RTSP_VIEWER(object) (G_TYPE_CHECK_INSTANCE_TYPE ((object), GST_RTSP_VIEWER_TYPE))
#define GST_IS_RTSP_VIEWER_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE ((klass), GST_RTSP_VIEWER_TYPE))
#define GST_RTSP_VIEWER_GET_CLASS(obj) (G_TYPE_INSTANCE_GET_CLASS ((obj), GST_RTSP_VIEWER_TYPE, GstRTSPViewerClass))

typedef struct _GstRTSPViewer GstRTSPViewer;
typedef struct _GstRTSPViewerClass GstRTSPViewerClass;
typedef struct _GstRTSPViewerPrivate GstRTSPViewerPrivate;

struct _GstRTSPViewer {
  GObject parent;

  /*< protected >*/

  /*< private >*/
};

struct _GstRTSPViewerClass {
  GObjectClass parent_class;

  /*< private >*/
};

GType gst_rtsp_viewer_get_type (void);

G_END_DECLS

#endif /* _GST_RTSP_VIEWER_H_ */
