package com.letssolvetogether.omr.object;

import androidx.lifecycle.ViewModel;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.core.Mat;

public class OMRSheet extends ViewModel {

    private static String TAG="OMRSheet";

    private Bitmap bmpOMRSheet;
    private Mat matOMRSheet;

    private int width;
    private int height;

    private OMRSheetCorners omrSheetCorners;
    private OMRSheetBlock omrSheetBlock;

    private int numberOfQuestions;
    private int optionsPerQuestions = 4;
    private int questionsPerBlock = 25;//25
    private int widthOfBoundingSquareForCircle;

    private int requiredBlackPixelsInBoundingSquare;
    private int totalPixelsInBoundingSquare;

    private int[] correctAnswers;

    public OMRSheet() {}

    public int getWidth(){
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight(){
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Bitmap getBmpOMRSheet() {
        return bmpOMRSheet;
    }

    public void setBmpOMRSheet(Bitmap bmpOMRSheet) {
        this.bmpOMRSheet = bmpOMRSheet;
    }

    public OMRSheetCorners getOmrSheetCorners() {
        return omrSheetCorners;
    }

    public void setOmrSheetCorners(OMRSheetCorners omrSheetCorners) {
        this.omrSheetCorners = omrSheetCorners;
    }

    public OMRSheetBlock getOmrSheetBlock() {
        return omrSheetBlock;
    }

    public void setOmrSheetBlock() {

        int w = getWidth();
        int h = getHeight();

        omrSheetBlock = new OMRSheetBlock();
        omrSheetBlock.setBlockWidth((int)(w/6.2));//original 6.0
        omrSheetBlock.setBlockHeight((int)(h/4.0));//original 5.0//

        Log.i(TAG,"BlockWidth - " + (int)(w/6.2));
        Log.i(TAG,"BlockHeight - " + (int)(h/4.0));

        omrSheetBlock.setxFirstBlockOffset((int)(w/4.5));//original 8.0
        omrSheetBlock.setyFirstBlockOffset((int)(h/5.5));//original 5.0

        Log.i(TAG,"xFirstBlockOffset - " + (int)(w/4.5));
        Log.i(TAG,"yFirstBlockOffset - " + (int)(h/5.5));

        omrSheetBlock.setxDistanceBetweenBlock((int)(w/4.0));//original 7.0
        omrSheetBlock.setyDistanceBetweenBlock(0);

        Log.i(TAG,"xDistanceBetweenBlock - " + (int)(w/4.0));
        Log.i(TAG,"yDistanceBetweenBlock - " + 0);

        omrSheetBlock.setyDistanceBetweenRows((int)(h/84.0));//original 100.0///90

        Log.i(TAG,"yDistanceBetweenRows - " + (int)(h/84.0));//90//84

        omrSheetBlock.setxDistanceBetweenCircles((int)(w/16.0));//original 25.0
        omrSheetBlock.setyDistanceBetweenCircles((int)(h/50.0));//original 50.0

        Log.i(TAG,"xDistanceBetweenCircles - " + (int)(w/16.0));//15
        Log.i(TAG,"yDistanceBetweenCircles - " + (int)(h/50.0));//45
    }

    public Mat getMatOMRSheet() {
        return matOMRSheet;
    }

    public void setMatOMRSheet(Mat matOMRSheet) {
        this.matOMRSheet = matOMRSheet;
    }

    public int getNumberOfBlocks() {
        return numberOfQuestions/ questionsPerBlock;
    }

    public int getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(int numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public int getOptionsPerQuestions() {
        return optionsPerQuestions;
    }

    public int getWidthOfBoundingSquareForCircle() {
        widthOfBoundingSquareForCircle = (int)(getWidth()/27.5);//original 30
        //The width should be even otherwise we will get width difference of 1 in getTotalPixelsInBoundingSquare()
        // as we divide the width in getRectangleCoordinates()
        if(widthOfBoundingSquareForCircle % 2 != 0)
            widthOfBoundingSquareForCircle++;
        return widthOfBoundingSquareForCircle;
    }

    public int getQuestionsPerBlock() {
        return questionsPerBlock;
    }

    public int getTotalPixelsInBoundingSquare() {
        totalPixelsInBoundingSquare = getWidthOfBoundingSquareForCircle() * getWidthOfBoundingSquareForCircle();
        return totalPixelsInBoundingSquare;
    }

    public int getRequiredBlackPixelsInBoundingSquare() {
        requiredBlackPixelsInBoundingSquare = (int) (getTotalPixelsInBoundingSquare() * 0.22);
        return requiredBlackPixelsInBoundingSquare;
    }

    public int[] getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int[] correctAnswers) {
        this.correctAnswers = correctAnswers;
    }
}