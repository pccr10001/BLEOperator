package adapter;

/**
 * Created by IDIC on 2017/1/4.
 */
public interface BLEServiceCallback<ResultType> {

    void supply(ResultType data);

}
