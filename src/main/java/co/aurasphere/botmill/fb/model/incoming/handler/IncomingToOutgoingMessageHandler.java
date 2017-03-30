package co.aurasphere.botmill.fb.model.incoming.handler;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import co.aurasphere.botmill.core.BotDefinition;
import co.aurasphere.botmill.core.annotation.Bot;
import co.aurasphere.botmill.core.internal.exception.BotMillEventMismatchException;
import co.aurasphere.botmill.core.internal.util.ConfigurationUtils;
import co.aurasphere.botmill.fb.event.AnyEvent;
import co.aurasphere.botmill.fb.event.FbBotMillEvent;
import co.aurasphere.botmill.fb.event.message.LocationEvent;
import co.aurasphere.botmill.fb.event.message.MessageEvent;
import co.aurasphere.botmill.fb.event.message.MessagePatternEvent;
import co.aurasphere.botmill.fb.event.message.QuickReplyMessageEvent;
import co.aurasphere.botmill.fb.event.message.QuickReplyMessagePatternEvent;
import co.aurasphere.botmill.fb.event.postback.PostbackEvent;
import co.aurasphere.botmill.fb.event.postback.PostbackPatternEvent;
import co.aurasphere.botmill.fb.model.annotation.FbBotMillController;
import co.aurasphere.botmill.fb.model.incoming.MessageEnvelope;


/**
 * The Class IncomingToOutgoingMessageHandler.
 */
public class IncomingToOutgoingMessageHandler {
	
	/** The instance. */
	private static IncomingToOutgoingMessageHandler instance;
	
	/** The Constant CONST_INCOMING_MSG_SETNAME. */
	private static final String CONST_INCOMING_MSG_SETNAME = "setEnvelope";
	
	/** The Constant CONST_EVENT_SETNAME. */
	private static final String CONST_EVENT_SETNAME = "setEvent";
	
	/**
	 * Creates the handler.
	 *
	 * @return the incoming to outgoing message handler
	 */
	public static IncomingToOutgoingMessageHandler createHandler() {
		if (instance == null) {
			instance = new IncomingToOutgoingMessageHandler();
		}
		return instance;
	}
	
	/**
	 * Process.
	 *
	 * @param message the message
	 * @return the incoming to outgoing message handler
	 */
	public IncomingToOutgoingMessageHandler process(MessageEnvelope message) {
		this.handleOutgoingMessage(message);
		return this;
	}
	
	/**
	 * Handle outgoing message.
	 *
	 * @param message the message
	 */
	private void handleOutgoingMessage(MessageEnvelope message) {
		
		for (BotDefinition defClass : ConfigurationUtils.getBotDefinitionInstance()) {
			if (defClass.getClass().isAnnotationPresent(Bot.class)) {
				for (Method method : defClass.getClass().getMethods()) {
					if (method.isAnnotationPresent(FbBotMillController.class)) {
						FbBotMillController botMillController = method.getAnnotation(FbBotMillController.class);
						try {
							FbBotMillEvent event = toEventActionFrame(botMillController);

							if (event.verifyEventCondition(message)) {
								
								defClass.getClass().getSuperclass()
										.getDeclaredMethod(CONST_INCOMING_MSG_SETNAME, MessageEnvelope.class)
										.invoke(defClass, message); 
								
								defClass.getClass().getSuperclass()
										.getDeclaredMethod(CONST_EVENT_SETNAME, FbBotMillEvent.class)
										.invoke(defClass, event);
								
								method.invoke(defClass, message);
								break;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	/**
	 * To event action frame.
	 *
	 * @param botMillController the bot mill controller
	 * @return the fb bot mill event
	 * @throws BotMillEventMismatchException the bot mill event mismatch exception
	 */
	private FbBotMillEvent toEventActionFrame(FbBotMillController botMillController)
			throws BotMillEventMismatchException {
		boolean caseSensitive = botMillController.caseSensitive();

		switch (botMillController.eventType()) {
		case MESSAGE:
			if (!botMillController.text().equals("")) {
				return new MessageEvent(botMillController.text(), caseSensitive);
			} else {
				throw new BotMillEventMismatchException("text attribute missing");
			}
		case MESSAGE_PATTERN:
			if (!botMillController.pattern().equals("")) {
				return new MessagePatternEvent(Pattern.compile(botMillController.pattern()));
			} else {
				throw new BotMillEventMismatchException("pattern attribute missing");
			}
		case POSTBACK:
			if (!botMillController.postback().equals("")) {
				return new PostbackEvent(botMillController.postback());
			} else {
				throw new BotMillEventMismatchException("postback attribute missing");
			}
		case POSTBACK_PATTERN:
			if (!botMillController.postbackPattern().equals("")) {
				return new PostbackPatternEvent(Pattern.compile(botMillController.postbackPattern()));
			} else {
				throw new BotMillEventMismatchException("postback pattern attribute missing");
			}
		case QUICK_REPLY_MESSAGE:
			if (!botMillController.quickReplyPayload().equals("")) {
				return new QuickReplyMessageEvent(botMillController.quickReplyPayload());
			} else {
				throw new BotMillEventMismatchException("quickpayload attribute missing");
			}
		case QUICK_REPLY_MESSAGE_PATTERN:
			if (!botMillController.quickReplyPayloadPattern().equals("")) {
				return new QuickReplyMessagePatternEvent(Pattern.compile(botMillController.quickReplyPayloadPattern()));
			} else {
				throw new BotMillEventMismatchException("quickpayload pattern attribute missing");
			}
		case LOCATION:
			return new LocationEvent();
		case ANY:
			return new AnyEvent();
		default:
			throw new BotMillEventMismatchException("Unsupported Event Type: " + botMillController.eventType());
		}
	}
}
