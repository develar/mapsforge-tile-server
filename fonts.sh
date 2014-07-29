#!/bin/sh
#for fontSize in 20 18 16 14
#do
#	GDCL font-regular.GlyphProject dist/fonts/12 -fs fontSize -fo XML-fnt
#done

for fontSize in 13 14 16
do
	GDCL font-italic.GlyphProject dist/fonts/$fontSize-i -fs $fontSize -fo XML-fnt
done

for fontSize in 8 9 10 11 12 13 14 15 16 18 20
do
	GDCL font-bold.GlyphProject dist/fonts/$fontSize-b -fs $fontSize -fo XML-fnt
done

for fontSize in 11 12 14
do
	GDCL font-bold-italic.GlyphProject dist/fonts/$fontSize-bi -fs $fontSize -fo XML-fnt
done