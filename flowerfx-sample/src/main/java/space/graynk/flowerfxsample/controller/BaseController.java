package space.graynk.flowerfxsample.controller;

import space.graynk.flowerfx.Swapper;
import space.graynk.flowerfx.interfaces.Swappable;

public class BaseController implements Swappable {
    protected Swapper swapper;

    @Override
    public void setSwapper(Swapper swapper) {
        this.swapper = swapper;
    }

    @Override
    public void reinitialize() {
        //no-op
    }
}
