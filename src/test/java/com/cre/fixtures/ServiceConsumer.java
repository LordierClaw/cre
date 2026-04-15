package com.cre.fixtures;

public class ServiceConsumer {
    private final RealService service = new RealService();

    public void consume() {
        service.doRealWork();      // On RealService
        service.doAbstractWork();  // On AbstractBaseService
        service.doInterfaceWork(); // On RealService (Override)
    }
}
