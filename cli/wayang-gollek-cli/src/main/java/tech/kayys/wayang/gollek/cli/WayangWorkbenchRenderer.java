package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangWorkbenchModel;

interface WayangWorkbenchRenderer<T> {

    T render(WayangWorkbenchModel model);
}
