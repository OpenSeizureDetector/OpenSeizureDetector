% 3-color version of upBlur

function out = blurUpClr(im, nlevs, filt)

%------------------------------------------------------------
%% OPTIONAL ARGS:

if (exist('nlevs') ~= 1) 
  nlevs = 1;
end

if (exist('filt') ~= 1) 
  filt = 'binom5';
end

%------------------------------------------------------------

tmp = upBlur(im(:,:,1), nlevs, filt);
out = zeros(size(tmp,1), size(tmp,2), size(im,3));
out(:,:,1) = tmp;
for clr = 2:size(im,3)
  out(:,:,clr) = upBlur(im(:,:,clr), nlevs, filt);
end
