package org.westmalle.launch;

import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component
public interface LauncherComponent {
    JvmLauncher jvmLauncher();

    DirectLauncherSubcomponent direct();

    IndirectLauncherSubcomponent indirect();
}
