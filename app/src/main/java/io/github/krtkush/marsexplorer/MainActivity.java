package io.github.krtkush.marsexplorer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.krtkush.marsexplorer.PicturesJsonDataModels.PhotoSearchResultDM;
import io.github.krtkush.marsexplorer.WeatherJsonDataModel.MarsWeatherDM;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private Subscriber<PhotoSearchResultDM> nasaMarsPhotoSubscriber;
    private Subscriber<MarsWeatherDM> maasMarsWeatherSubscriber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Timber.tag(MainActivity.this.getClass().getSimpleName());
    }

    @OnClick(R.id.sendRequest)
    public void sendNetworkRequest() {

        getMaxSol(GeneralConstants.Curiosity);
        getMaxSol(GeneralConstants.Opportunity);
        getMaxSol(GeneralConstants.Spirit);
        getWeather();
    }

    private void getWeather() {

        Observable<MarsWeatherDM> marsWeatherDMObservable
                = MarsExplorer.getApplicationInstance()
                .getMaasWeatherApiInterface()
                .getLatestMarsWeather(true);

        maasMarsWeatherSubscriber = new Subscriber<MarsWeatherDM>() {
            @Override
            public void onCompleted() {
                Timber.i("Weather found");
            }

            @Override
            public void onError(Throwable ex) {
                ex.printStackTrace();
            }

            @Override
            public void onNext(MarsWeatherDM marsWeatherDM) {
                marsWeatherDM.getReport().getMaxTemp();
            }
        };

        marsWeatherDMObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(maasMarsWeatherSubscriber);
    }

    /**
     * Method to get the max possible SOL for a specified rover.
     * The API is hit for SOL 1, from which the max SOL is extracted.
     * @param roverName Name of the rover for which max SOL is needed.
     */
    private void getMaxSol(final String roverName) {

        //Define the observer
        Observable<PhotoSearchResultDM> nasaMarsPhotosObservable
                = MarsExplorer.getApplicationInstance()
                .getNasaMarsPhotosApiInterface()
                .getPhotosBySol(true, true, roverName, "1", null);

        //Define the subscriber
        nasaMarsPhotoSubscriber = new Subscriber<PhotoSearchResultDM>() {
            @Override
            public void onCompleted() {
                Timber.i("Max SOL of %s found", roverName);
            }

            @Override
            public void onError(Throwable ex) {
                ex.printStackTrace();
            }

            @Override
            public void onNext(PhotoSearchResultDM photoSearchResultDM) {
                photoSearchResultDM.getPhotos().get(0).getRover().getMaxSol();
            }
        };

        //Subscribe to the observable
        nasaMarsPhotosObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(nasaMarsPhotoSubscriber);
    }

    @Override
    protected void onStop() {
        super.onStop();

        try {
            nasaMarsPhotoSubscriber.unsubscribe();
            maasMarsWeatherSubscriber.unsubscribe();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }
}
