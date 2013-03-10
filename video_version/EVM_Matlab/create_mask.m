% MASK = create_mask(IMAGE)
% 
% Using imply function built in MATLAB to create mask
% for localized Eulerian video processing
% 
% Copyright (c) 2011-2012 Massachusetts Institute of Technology, 
% Quanta Research Cambridge, Inc.
%
% Authors: Hao-yu Wu, Michael Rubinstein, Eugene Shih, 
% License: Please refer to the LICENCE file
% Date: June 2012
%
function mask = create_mask( image )
    
    imshow(image);

    h = impoly;
    position = wait(h);
    BW = createMask(h);
    mask = zeros(size(BW));
    mask(BW) = 1;
    
    % blur the mask to produce visually better result
    g = fspecial('gaussian',15,5);
    mask = imfilter(mask,g);
    
    mask = repmat(mask,[1 1 3]);
    save([vidName '.mat'],'mask');
    
end

