% amplify_spatial_lpyr_temporal_ideal(vidFile, outDir, alpha, lambda_c,
%                                     wl, wh, samplingRate, chromAttenuation)
% 
% Spatial Filtering: Laplacian pyramid
% Temporal Filtering: Ideal bandpass
% 
% Copyright (c) 2011-2012 Massachusetts Institute of Technology, 
% Quanta Research Cambridge, Inc.
%
% Authors: Hao-yu Wu, Michael Rubinstein, Eugene Shih, 
% License: Please refer to the LICENCE file
% Date: June 2012
%
function amplify_spatial_lpyr_temporal_ideal(vidFile, outDir ...
    ,alpha, lambda_c, fl, fh ...
    ,samplingRate, chromAttenuation)
    

    [~,vidName] = fileparts(vidFile);

    outName = fullfile(outDir,[vidName '-ideal-from-' num2str(fl) ...
                       '-to-' num2str(fh) '-alpha-' num2str(alpha) ...
                       '-lambda_c-' num2str(lambda_c) '-chromAtn-' ...
                       num2str(chromAttenuation) '.avi']);

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


    % compute Laplacian pyramid for each frame
    [pyr_stack, pind] = build_Lpyr_stack(vidFile, startIndex, endIndex);

    % save the result for convenience

    % save(['pyrStack_' vidName '.mat'],'pyr_stack','pind','-v7.3');

    filtered_stack = ideal_bandpassing(pyr_stack, 3, fl, fh, samplingRate);


    %% amplify each spatial frequency bands according to Figure 6 of our paper
    ind = size(pyr_stack(:,1,1),1);
    nLevels = size(pind,1);
    
    delta = lambda_c/8/(1+alpha);
    
    % the factor to boost alpha above the bound we have in the
    % paper. (for better visualization)
    exaggeration_factor = 2;
    
    % compute the representative wavelength lambda for the lowest spatial 
    % freqency band of Laplacian pyramid
    
    lambda = (vidHeight^2 + vidWidth^2).^0.5/3; % 3 is experimental constant

    for l = nLevels:-1:1
      indices = ind-prod(pind(l,:))+1:ind;
      % compute modified alpha for this level
      currAlpha = lambda/delta/8 - 1;
      currAlpha = currAlpha*exaggeration_factor;
          
      if (l == nLevels || l == 1) % ignore the highest and lowest frequency band
          filtered_stack(indices,:,:) = 0;
      elseif (currAlpha > alpha)  % representative lambda exceeds lambda_c
          filtered_stack(indices,:,:) = alpha*filtered_stack(indices,:,:);
      else
          filtered_stack(indices,:,:) = currAlpha*filtered_stack(indices,:,:);
      end
      
      ind = ind - prod(pind(l,:));
      % go one level down on pyramid, 
      % representative lambda will reduce by factor of 2
      lambda = lambda/2; 
    end
    
    %% Render on the input video

    % output video
    k = 0;
    for i=startIndex+1:endIndex
        i
        k = k+1;
        temp.cdata = read(vid, i);
        [rgbframe,~] = frame2im(temp);
        rgbframe = im2double(rgbframe);
        frame = rgb2ntsc(rgbframe);

        filtered = zeros(vidHeight,vidWidth,3);

        filtered(:,:,1) = reconLpyr(filtered_stack(:,1,k),pind);
        filtered(:,:,2) = reconLpyr(filtered_stack(:,2,k),pind)*chromAttenuation;
        filtered(:,:,3) = reconLpyr(filtered_stack(:,3,k),pind)*chromAttenuation;

        filtered = filtered+frame;

        frame = ntsc2rgb(filtered);

        frame(frame > 1) = 1;
        frame(frame < 0) = 0;


        writeVideo(vidOut,im2uint8(frame));
    end


    close(vidOut);

end
