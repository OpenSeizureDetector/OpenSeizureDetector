% [LPYR_STACK, pind] = build_Lpyr_stack(VID_FILE, START_INDEX, END_INDEX)
% 
% Apply Laplacian pyramid decomposition on vidFile from startIndex to
% endIndex
% 
% LPYR_STACK: stack of Laplacian pyramid of each frame 
% the second dimension is the color channel
% the third dimension is the time
%
% pind: see buildLpyr function in matlabPyrTools library
% 
% Copyright (c) 2011-2012 Massachusetts Institute of Technology, 
% Quanta Research Cambridge, Inc.
%
% Authors: Hao-yu Wu, Michael Rubinstein, Eugene Shih, 
% License: Please refer to the LICENCE file
% Date: June 2012
%
function [Lpyr_stack, pind] = build_Lpyr_stack(vidFile, startIndex, endIndex)

    % Read video
    vid = VideoReader(vidFile);
    % Extract video info
    vidHeight = vid.Height;
    vidWidth = vid.Width;
    nChannels = 3;
    temp = struct('cdata', zeros(vidHeight, vidWidth, nChannels, 'uint8'), 'colormap', []);


    % firstFrame
    temp.cdata = read(vid, startIndex);
    [rgbframe,~] = frame2im(temp);
    rgbframe = im2double(rgbframe);
    frame = rgb2ntsc(rgbframe);

    [pyr,pind] = buildLpyr(frame(:,:,1),'auto');

    % pre-allocate pyr stack
    Lpyr_stack = zeros(size(pyr,1),3,endIndex - startIndex +1);
    Lpyr_stack(:,1,1) = pyr;

    [Lpyr_stack(:,2,1), ~] = buildLpyr(frame(:,:,2),'auto');
    [Lpyr_stack(:,3,1), ~] = buildLpyr(frame(:,:,3),'auto');

    k = 1;
    for i=startIndex+1:endIndex
            k = k+1;
            temp.cdata = read(vid, i);
            [rgbframe,~] = frame2im(temp);

            rgbframe = im2double(rgbframe);
            frame = rgb2ntsc(rgbframe);

            [Lpyr_stack(:,1,k),~] = buildLpyr(frame(:,:,1),'auto');
            [Lpyr_stack(:,2,k),~] = buildLpyr(frame(:,:,2),'auto');
            [Lpyr_stack(:,3,k),~] = buildLpyr(frame(:,:,3),'auto');
    end
end
