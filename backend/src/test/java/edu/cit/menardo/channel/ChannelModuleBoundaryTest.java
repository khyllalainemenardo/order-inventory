package edu.cit.menardo.channel;

import java.lang.reflect.Modifier;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChannelModuleBoundaryTest {

    @Test
    void onlyTheInterfaceAndOurOwnTypesArePublic() {
        List<Class<?>> publicTypes = List.of(SalesChannel.class, ChannelStatus.class);
        List<Class<?>> internalTypes = List.of(
                TianggeClient.class, TianggeJson.class, TianggeException.class, TianggeTranslator.class,
                FeedPoller.class, FeedEventHandler.class, TianggeOutbox.class, TianggeConnection.class,
                ChannelEventListener.class, TianggeSalesChannel.class, ChannelController.class,
                ChannelOrder.class, ChannelOrderRepository.class, ChannelDecision.class,
                FeedCursor.class, FeedCursorRepository.class, ProcessedEvent.class, ProcessedEventRepository.class);

        publicTypes.forEach(type -> assertThat(Modifier.isPublic(type.getModifiers())).as(type.getSimpleName()).isTrue());
        internalTypes.forEach(type -> assertThat(Modifier.isPublic(type.getModifiers())).as(type.getSimpleName()).isFalse());
        for (Class<?> json : TianggeJson.class.getDeclaredClasses()) {
            assertThat(Modifier.isPublic(json.getModifiers())).as(json.getSimpleName()).isFalse();
        }
    }
}
