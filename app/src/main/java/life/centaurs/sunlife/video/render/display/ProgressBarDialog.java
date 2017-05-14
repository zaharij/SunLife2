package life.centaurs.sunlife.video.render.display;


import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ProgressBarDialog {
    private final int MAX_STATUS_NUMBER = 100;
    private ImageView backgroundImageView;
    private ProgressBar progressBar;
    private TextView progressText;
    private Button cancelButton, okButton, cancelButton2, okButton2;
    private int progressStatus;
    private int oneUpdateProgressStatus;

    enum ProgressBarDialogVisibilityEnum{
        VISIBLE, INVISIBLE;
    }

    public ProgressBarDialog(ImageView backgroundImageView, ProgressBar progressBar
            , TextView progressText, Button cancelButton, Button okButton) {
        this.backgroundImageView = backgroundImageView;
        this.progressBar = progressBar;
        this.progressText = progressText;
        this.cancelButton = cancelButton;
        this.okButton = okButton;
        this.okButton.setEnabled(false);
    }

    public ProgressBarDialog(ImageView backgroundImageView, ProgressBar progressBar, TextView progressText
            , Button cancelButton, Button okButton, Button cancelButton2, Button okButton2) {
        this.backgroundImageView = backgroundImageView;
        this.progressBar = progressBar;
        this.progressText = progressText;
        this.cancelButton = cancelButton;
        this.okButton = okButton;
        this.cancelButton2 = cancelButton2;
        this.okButton2 = okButton2;
    }

    public void setEachUpdateProgressStatus(int oneUpdateProgressStatus) {
        this.oneUpdateProgressStatus = (MAX_STATUS_NUMBER / oneUpdateProgressStatus) + 1;
    }

    public void updateProgress(){
        setProgressStatus(oneUpdateProgressStatus);

    }

    public void setProgressStatus(int progressStatus) {
        int temp = this.progressStatus + progressStatus;
        if (temp >= MAX_STATUS_NUMBER){
            this.progressStatus = MAX_STATUS_NUMBER;
            this.okButton.setEnabled(true);
            setProgress(MAX_STATUS_NUMBER);
        }
        else {
            this.progressStatus = temp;
            setProgress(temp);
        }
    }

    public void setTextToCancelButton(String text){
        cancelButton.setText(text);
    }

    public void setTextToOkButton(String text){
        okButton.setText(text);
    }

    public void setTextToCancelButton2(String text){
        cancelButton2.setText(text);
    }

    public void setTextToOkButton2(String text){
        okButton2.setText(text);
    }

    private void setProgress(int progressNumber){
        progressBar.setProgress(progressNumber);
        progressText.setText("" + progressNumber + "%");
    }

    public void setProgressText(String text){
        progressText.setText(text);
    }

    public void showOnEndProgressChoiceDialog(){
        cancelButton.setVisibility(View.INVISIBLE);
        okButton.setVisibility(View.INVISIBLE);
        cancelButton2.setVisibility(View.VISIBLE);
        okButton2.setVisibility(View.VISIBLE);
    }

    public void setVisibility(ProgressBarDialogVisibilityEnum progressBarDialogVisibilityEnum){
        switch (progressBarDialogVisibilityEnum){
            case VISIBLE:
                backgroundImageView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                progressText.setVisibility(View.VISIBLE);
                cancelButton.setVisibility(View.VISIBLE);
                okButton.setVisibility(View.VISIBLE);
                break;
            case INVISIBLE:
                backgroundImageView.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
                progressText.setVisibility(View.INVISIBLE);
                cancelButton.setVisibility(View.INVISIBLE);
                okButton.setVisibility(View.INVISIBLE);
                try {
                    cancelButton2.setVisibility(View.INVISIBLE);
                    okButton2.setVisibility(View.INVISIBLE);
                } catch (NullPointerException e){
                }
                break;
        }
    }
}
