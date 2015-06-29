/*
 * Copyright (c) 2011-2014 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
/**
 * = Vert.x Mail client (SMTP client implementation)
 *
 * Vert.x client for sending SMTP emails via a local mail server
 * (e.g. postfix) or by external mail server (e.g. googlemail or aol).
 *
 * The client supports a few additional auth methods like DIGEST-MD5 and has full
 * support for TLS and SSL and is completely asynchronous. The client supports
 * connection pooling to keep connections open to be reused.
 *
 * == Creating a client
 *
 * You can send mails by creating a client that opens SMTP connections from the local jvm.
 *
 * The client uses a configuration object, the default config is created as empty
 * object and will connect to localhost port 25, which should be ok in a standard
 * Linux environment where you have Postfix or similar mail server running on
 * the local machine. For all possible properties of the config object, see below.
 *
 * The client can use a connection pool of the SMTP connections to get rid of the overhead of
 * connecting each time to the server, negotiating TLS and login (this function can be
 * turned off by setting keepAlive = false). A client can either be shared or non-shared,
 * if it is shared, the same connection pool will be used for all clients using the same identifier.
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#createSharedClient}
 * ----
 * The first call to MailClient.createShared will actually create the pool with the specified config.
 * Subsequent calls will return a new client instance that uses the same pool, so the configuration won't be used.
 *
 * If you leave out the pool identifier, a default pool will be created. Note that the clients are
 * shared in the scope of a vertx instance only (so two different vertx will have different pools with the
 * same identifier).
 *
 * The unshared client can be created the same way leaving out the identifier.
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#createNonSharedClient}
 * ----
 *
 * A more elaborate example using a mailserver that requires login via TLS
 * [source,$lang]
 * ----
 * {@link examples.Examples#createClient2}
 * ----
 *
 * == Sending mails
 *
 * Once the client object is created, you can use it to send mails. Since the
 * sending of the mails works asynchronous in vert.x, the result handler will be
 * called when the mail operation finishes. You can start many mail send operations
 * in parallel, the connection pool will limit the number of concurrent operations
 * so that new operations will wait in queue if no slots are available.
 * 
 * A mail message is constructed as JSON. The MailMessage object has
 * properties from, to, cc, bcc, subject, text, html etc. Depending on which values are set, the
 * format of the generated MIME message will vary. The recipient address properties
 * can either be a single address or a list of addresses.
 *
 * The MIME encoder supports us-ascii (7bit) headers/messages and utf8 (usually
 * quoted-printable) headers/messages
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#mailMessage}
 * ----
 *
 * Attachments can be created by the MailAttachment object using data stored in a Buffer,
 * this supports base64 attachments.
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#attachment}
 * ----
 * When sending the mail, you can provide a AsyncResult<MailResult> handler that will be called when
 * the send operation is finished or it failed.
 *
 * A mail is sent as follows:
 *
 * [source,$lang]
 * ----
 * {@link examples.Examples#sendMail}
 * ----
 *
 * == Mail-client data objects
 *
 * === MailMessage properties
 *
 * Email fields are Strings using the common formats for email with or without real
 * name
 *
 * * `username@example.com`
 * * `username@example.com (Firstname Lastname)`
 * * `Firstname Lastname <username@example.com>`
 *
 * The MailMessage object has the following properties
 *
 * * `from` String representing the From address and the MAIL FROM field
 * * `to` String or list of String representing the To addresses and the RCPT TO fields
 * * `cc` same as to
 * * `bcc` same as to
 * * `bounceAddress` String representing the error address (MAIL FROM), if not set from is used
 * * `text` String representing the text/plain part of the mail
 * * `html` String representing the text/html part of the mail
 * * `attachment` MailAttachment or list of MailAttachment attachments of the message
 * * `headers` MultiMap representing headers to be added in addition to the headers necessary for the MIME Message
 * * `fixedHeaders` boolean if true, only the headers provided as headers property will be set in the generated message
 *
 * the last two properties allow manipulating the generate messages with custom headers, e.g. providing
 * a message-id chosen by the calling program or setting different headers than would be generated by default. Unless you know
 * what you are doing, this may generate invalid messages.
 *
 * === MailAttachment properties
 * The MailAttachment object has the following properties
 *
 * * `data` Buffer containing the binary data of the attachment
 * * `contentType` String of the Content-Type of the attachment (e.g. text/plain or text/plain; charset="UTF8", default is application/octet-stream)
 * * `description` String describing the attachment (this is put in the description header of the attachment), optional
 * * `disposition` String describing the disposition of the attachment (this is either "inline" or "attachment", default is attachment)
 * * `name` String filename of the attachment (this is put into the disposition and in the Content-Type headers of the attachment), optional
 *
 * === MailConfig options
 *
 * The configuration has the following properties
 *
 * * `hostname` the hostname of the smtp server to connect to (default is localhost)
 * * `port` the port of the smtp server to connect to (default is 25)
 * * `startTLS` StartTLSOptions either DISABLED, OPTIONAL or REQUIRED, default is OPTIONAL
 * * `login` LoginOption either DISABLED, NONE or REQUIRED, default is NONE
 * * `username` String of the username to be used for login
 * * `password` String of the password to be used for login
 * * `ssl` boolean whether to use ssl on connect to the mail server (default is false), set this to use a port 465 ssl connection
 * * `ehloHostname` String to used in EHLO and for creating the message-id, if not set, the own hostname will be used, which may not be a good choice if it doesn't contain a FQDN or is localhost
 * * `authMethods` String space separated list of allowed auth methods, this can be used to disallow some auth methods or define one required auth method
 * * `keepAlive` boolean if connection pooling is enabled (default is true)
 * * `maxPoolSize` int max number of open connections kept in the pool or to be opened at one time (regardless if pooling is enabled or not), default is 10
 * * `trustAll` boolean whether to accept all certs from the server (default is false)
 * * `keyStore` String the key store filename, this can be used to trust a server cert that is custom generated
 * * `keyStorePassword` String password used to decrypt the key store
 * * `allowRcptErrors` boolean if true, sending continues if a recipient address is not accepted and the mail will be sent if at least one address is accepted
 *
 * === MailResult object
 * The MailResult object has the following members
 *
 * * `messageID` the Message-ID of the generated mail
 * * `recipients` the list of recipients the mail was sent to (if allowRcptErrors is true, this may be fewer than the intended recipients)
 *
 */
@Document(fileName = "index.adoc")
@GenModule(name = "vertx-mail")
package io.vertx.ext.mail;

import io.vertx.codegen.annotations.GenModule;
import io.vertx.docgen.Document;

