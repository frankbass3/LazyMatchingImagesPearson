# Lazy Matching Pattern image using the Cross Corrlation Pearson coefficient (imagej plugin)

Lazy Matching Pattern image using the Cross Corrlation Pearson coefficient.
It use imagej as image processor and given an image and a pattern Or N pattern. it perform recursively on the entire image on the size of the pattern the matching between the pattern and the covered part of image. The matching is performed using https://en.wikipedia.org/wiki/Cross-correlation#Normalized_cross-correlation. This allow to use a lazy learning algorithm https://en.wikipedia.org/wiki/Instance-based_learning as KNN for facial expression detection. So the matching is perfomred for each given pattern(facial expression) and by voiting will output the best Roi of the image found that match the pattern.
