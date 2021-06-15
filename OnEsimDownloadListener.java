package md.moldcell.selfservice.utils.esim;

public interface OnEsimDownloadListener {

    void onEsimSuccess(String result);

    void onEsimFailure(String result);

}