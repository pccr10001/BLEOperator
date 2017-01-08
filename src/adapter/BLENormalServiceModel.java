package adapter;

public interface BLENormalServiceModel<ResultType> {

    boolean init();

    ResultType convert(byte[] bytes);

}
