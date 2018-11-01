import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.cluster.pubsub.DistributedPubSub;
import akka.cluster.pubsub.DistributedPubSubMediator;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetAddress;

import static akka.pattern.PatternsCS.ask;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

import static org.apache.commons.codec.binary.Hex.*;

/**
 * @author 16779246
 * @since 29.10.18
 */
public class Listener extends AbstractActor {

	private static abstract class Reply {
	}

	private static final class Accept extends Reply implements Serializable {
		private String path;

		public Accept(String path) {
			this.path = path;
		}

		public String getPath() {
			return path;
		}
	}

	private static final class Refuse implements Serializable {
	}

	private static final Refuse REFUSE = new Refuse();

	private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	private Cluster cluster = Cluster.get(getContext().getSystem());
	private ConcurrentHashMap<String, String> peers = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, FileRequest> requests = new ConcurrentHashMap<>();
	private JFrame window = new JFrame();
	private JList<String> peerList = new JList<>();

	public Listener() {
		Container contentPane = window.getContentPane();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(peerList, BorderLayout.CENTER);
		JButton clicker = new JButton("send");
		contentPane.add(clicker, BorderLayout.SOUTH);
		clicker.addActionListener(event -> {
			getSelf().tell(new File("settings.gradle"), null);
		});
	}

	//subscribe to cluster changes
	@Override
	public void preStart() {
		cluster.subscribe(getSelf(), ClusterEvent.MemberUp.class);
		ActorRef mediator = DistributedPubSub.get(cluster.system()).mediator();
		mediator.tell(new DistributedPubSubMediator.Subscribe("welcome", getSelf()),
				getSelf());
		mediator.tell(new DistributedPubSubMediator.Put(getSelf()), getSelf());
		window.setVisible(true);
	}

	//re-subscribe when restart
	@Override
	public void postStop() {
		window.setVisible(false);
		cluster.unsubscribe(getSelf());
		DistributedPubSub.get(cluster.system()).mediator().tell(new DistributedPubSubMediator.Unsubscribe("welcome", getSelf()), getSelf());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				       .match(ClusterEvent.MemberUp.class, mUp -> {
					       InetAddress local = InetAddress.getLocalHost();
					       log.info("Member is Up: {}/:::/{}", mUp.member(), getSender());
					       ActorRef mediator = DistributedPubSub.get(cluster.system()).mediator();
					       CertMsg certMsg = new CertMsg(local.getCanonicalHostName(), String.valueOf(local.getHostAddress()), "localhost" + hashCode(), getSelf().path().toString(), null);
					       mediator.tell(new DistributedPubSubMediator.Publish("welcome", certMsg), getSelf());
				       })
				       .match(ClusterEvent.UnreachableMember.class, mUnreachable ->
						                                                    log.info("Member detected as unreachable: {}", mUnreachable.member())
				       )
				       .match(ClusterEvent.MemberRemoved.class, mRemoved ->
						                                                log.info("Member is Removed: {}", mRemoved.member())
				       )
				       .match(ClusterEvent.MemberEvent.class, message -> {
					       // ignore
				       })
				       .match(DistributedPubSubMediator.SubscribeAck.class, ack -> {
					       ActorRef mediator = DistributedPubSub.get(cluster.system()).mediator();
					       mediator.tell(new DistributedPubSubMediator.Publish("welcome", String.valueOf(System.currentTimeMillis())), getSelf());
				       })
				       .match(CertMsg.class, message -> {
					       log.info("Got message: " + message);
					       //store peer in list
					       addPeer(message.getAlias(), message.getAddress());
					       File file = new File("build.gradle");
					       FileRequest msg = new FileRequest(file);
					       ActorSelection selection = cluster.system().actorSelection(message.getAddress());
					       ActorRef self = getSelf();
					       InetAddress local = InetAddress.getLocalHost();
					       CertReply certMsg = new CertReply(local.getCanonicalHostName(), String.valueOf(local.getHostAddress()), "localhost" + hashCode(), getSelf().path().toString(), null);
					       getSender().tell(certMsg, self);
					       ask(selection, msg, Duration.ofMinutes(1)).toCompletableFuture().thenAccept(result -> {
						       String host = selection.pathString();
						       if (result instanceof Accept) {
							       log.info(String.format("%s accepted %s", host, file.getAbsolutePath()));
							       selection.tell(new FileData(msg.getData(), ((Accept) result).getPath()), self);
						       } else {
							       log.info(String.format("%s refused %s", host, file.getAbsolutePath()));
						       }
					       });
				       })
				       .match(CertReply.class, message -> {
					       log.info("Got reply: " + message);
					       //store peer in list
					       addPeer(message.getAlias(), message.getAddress());
				       })
				       .match(DistributedPubSubMediator.SubscribeAck.class, msg -> log.info("subscribed"))
				       .match(FileRequest.class, request -> {
					       Random rnd = new Random();
					       if (rnd.nextBoolean()) {
						       String hash = encodeHexString(request.getHash());
						       String uuid = UUID.randomUUID().toString();
						       log.info(String.format("accepting file %s of %s bytes with hash %s", request.getFileName(), request.getSize(), hash));
						       log.info(getSender().path().toString());
						       requests.put(uuid, request);
						       getSender().tell(new Accept(uuid), getSelf());
					       } else {
						       getSender().tell(REFUSE, getSelf());
					       }
				       })
					   .match(FileData.class, data -> {
						   FileRequest request = requests.remove(data.getId());
						   File file = new File(new File(System.getProperty("user.dir")).getParentFile(), data.getId() + request.getFileName());
						   OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
						   byte[] buffer = data.getData();
						   MessageDigest digest = MessageDigest.getInstance("sha-512");
						   digest.update(buffer);
						   log.info(String.format("%s: %s", file.getAbsolutePath(), Arrays.equals(digest.digest(), request.getHash())));
						   out.write(buffer);
						   out.flush();
						   out.close();
					   })
					   .match(File.class, file -> {
						   String selectedValue = peerList.getSelectedValue();
						   if (selectedValue != null) {
							   FileRequest msg = new FileRequest(file);
							   ActorSelection selection = cluster.system().actorSelection(peers.get(selectedValue));
							   ActorRef self = getSelf();
							   ask(selection, msg, Duration.ofMinutes(1)).toCompletableFuture().thenAccept(result -> {
								   String host = selection.pathString();
								   if (result instanceof Accept) {
									   log.info(String.format("%s accepted %s", host, file.getAbsolutePath()));
									   selection.tell(new FileData(msg.getData(), ((Accept) result).getPath()), self);
								   } else {
									   log.info(String.format("%s refused %s", host, file.getAbsolutePath()));
								   }
							   });
						   }
					   })
				       .matchAny(obj -> log.error("Got " + obj))
				       .build();
	}

	private void addPeer(String alias, String address) {
		peers.put(alias, address);
		peerList.setListData(peers.keySet().toArray(new String[0]));
		window.pack();
	}
}
