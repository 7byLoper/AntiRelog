package ru.leymooo.antirelog;

import ru.leymooo.antirelog.version.AntiRelogVersionAdapter;
import ru.leymooo.antirelog.version.VersionAdapter;

public final class AntiRelog extends AntiRelogPlugin {
    @Override
    protected VersionAdapter createVersionAdapter() {
        return new AntiRelogVersionAdapter();
    }
}
