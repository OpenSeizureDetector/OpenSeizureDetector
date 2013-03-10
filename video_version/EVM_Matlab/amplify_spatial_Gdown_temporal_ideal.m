% amplify_spatial_Gdown_temporal_ideal(vidFile, outDir, alpha, 
%                                      level, fl, fh, samplingRate, 
%                                      chromAttenuation)
%
% Spatial Filtering: Gaussian blur and down sample
% Temporal Filtering: Ideal bandpass
% 
% Copyright (c) 2011-2012 Massachusetts Institute of Technology, 
% Quanta Research Cambridge, Inc.
%
% Authors: Hao-yu Wu, Michael Rubinstein, Eugene Shih, 
% License: Please refer to the LICENCE file
% Date: June 2012
%
function amplify_spatial_Gdown_temporal_ideal(vidFile,outDir,alpha,level, ...
                     fl,fh,samplingRate, chromAttenuation)
 

    [~,vidName] = fileparts(vidFile);

    outName = fullfile(outDir,[vidName '-ideal-from-' num2str(fl) ...
                           '-to-' num2str(fh) ...
                           '-alpha-' num2str(alpha) ...
                           '-level-' num2str(level) ...
                           '-chromAtn-' num2str(chromAttenuation) '.avi']);


    % Read video
    vid = VideoReader(vidFile);
    % Extract video info
    vidHeight = vid.Height;
    vidWidth = vid.Width;
    nChannels = 3;
    fr = vid.FrameRate;
    len = vid.NumberOfFrames;
    temp = struct('cdata', zeros(vidHeight, vidWidth, nChannels, 'uint8'), 'colormap', []);

    startIndex = 1;
    endIndex = len-10;

    vidOut = VideoWriter(outName);
    vidOut.FrameRate = fr;

    open(vidOut)


    % compute Gaussian blur stack
    disp('Spatial filtering...')
    Gdown_stack = build_GDown_stack(vidFile, startIndex, endIndex, level);
    disp('Finished')
    
    
    % Temporal filtering
    disp('Temporal filtering...')
    filtered_stack = ideal_bandpassing(Gdown_stack, 1, fl, fh, samplingRate);
    disp('Finished')
    
    %% amplify
    filtered_stack(:,:,:,1) = filtered_stack(:,:,:,1) .* alpha;
    filtered_stack(:,:,:,2) = filtered_stack(:,:,:,2) .* alpha .* chromAttenuation;
    filtered_stack(:,:,:,3) = filtered_stack(:,:,:,3) .* alpha .* chromAttenuation;



    %% Render on the input video
    disp('Rendering...')
    % output video
    k = 0;
    for i=startIndex:endIndex
        k = k+1
        temp.cdata = read(vid, i);
        [rgbframe,~] = frame2im(temp);
        rgbframe = im2double(rgbframe);
        frame = rgb2ntsc(rgbframe);

        filtered = squeeze(filtered_stack(k,:,:,:));

        filtered = imresize(filtered,[vidHeight vidWidth]);

        filtered = filtered+frame;

        frame = ntsc2rgb(filtered);

        frame(frame > 1) = 1;
        frame(frame < 0) = 0;


        writeVideo(vidOut,im2uint8(frame));
    end

    disp('Finished')
    close(vidOut);

end
