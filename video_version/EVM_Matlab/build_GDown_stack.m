% GDOWN_STACK = build_GDown_stack(VID_FILE, START_INDEX, END_INDEX, LEVEL)
% 
% Apply Gaussian pyramid decomposition on VID_FILE from START_INDEX to
% END_INDEX and select a specific band indicated by LEVEL
% 
% GDOWN_STACK: stack of one band of Gaussian pyramid of each frame 
% the first dimension is the time axis
% the second dimension is the y axis of the video
% the third dimension is the x axis of the video
% the forth dimension is the color channel
% 
% Copyright (c) 2011-2012 Massachusetts Institute of Technology, 
% Quanta Research Cambridge, Inc.
%
% Authors: Hao-yu Wu, Michael Rubinstein, Eugene Shih, 
% License: Please refer to the LICENCE file
% Date: June 2012
%
function GDown_stack = build_GDown_stack(vidFile, startIndex, endIndex, level)

    % Read video
    vid = VideoReader(vidFile);
    % Extract video info
    vidHeight = vid.Height;
    vidWidth = vid.Width;
    nChannels = 3;
    temp = struct('cdata', zeros(vidHeight, vidWidth, nChannels, 'uint8'), 'colormap', []);

    % firstFrame
    temp.cdata = read(vid, startIndex);
    [rgbframe, ~] = frame2im(temp);
    rgbframe = im2double(rgbframe);
    frame = rgb2ntsc(rgbframe);

    blurred = blurDnClr(frame,level);

    % create pyr stack
    GDown_stack = zeros(endIndex - startIndex +1, size(blurred,1),size(blurred,2),size(blurred,3));
    GDown_stack(1,:,:,:) = blurred;

    k = 1;
    for i=startIndex+1:endIndex
            k = k+1;
            temp.cdata = read(vid, i);
            [rgbframe,~] = frame2im(temp);

            rgbframe = im2double(rgbframe);
            frame = rgb2ntsc(rgbframe);

            blurred = blurDnClr(frame,level);
            GDown_stack(k,:,:,:) = blurred;

    end
    
end
