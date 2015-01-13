package com.ctc.aem.site.core.products.importers.ftp.impl;

import static com.ctc.aem.site.core.products.importers.ftp.impl.FileArchiveUtils.unzipArchive;
import static org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE;

import com.ctc.aem.site.core.products.importers.ftp.FtpConfigurationProvider;
import com.ctc.aem.site.core.products.importers.ftp.FtpLoaderClient;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;
import java.util.Vector;
import java.util.stream.Stream;

public class ProductFromFtpImporter {

    private static final Logger LOG = LoggerFactory.getLogger(ProductFromFtpImporter.class);

    private static final String STRING_HOST_KEY_CHECKING = "StrictHostKeyChecking";
    private static final String NO = "no";
    private static final String FILE_EXTENTION = ".zip";
    private static final String SFTP_PREFIX = "sftp";

    private FtpConfigurationProvider ftpConfigurationProvider;

    public ProductFromFtpImporter(FtpConfigurationProvider ftpConfigurationProvider) {
        this.ftpConfigurationProvider = ftpConfigurationProvider;

    }


    public void startCopyFileToLocalDir() {

        try {
            if (!ftpConfigurationProvider.isUseSftp()) {
                ftpLoaderClient.copyFtpFilesToLocalDir();
            } else {
                sftpLoaderClient.copyFtpFilesToLocalDir();
            }


        } catch (Exception e) {
            LOG.error("Loader could not downloaded files", e);
        }

    }

    FtpLoaderClient sftpLoaderClient = () -> {
        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        LOG.info("preparing the host information for sftp.");
        FileOutputStream ftpFileToLocalDir = null;
        try {

            JSch jsch = new JSch();
            session = jsch.getSession(ftpConfigurationProvider.getLogin(), ftpConfigurationProvider.getHost(),
                                      Integer.parseInt(ftpConfigurationProvider.getPort()));
            session.setPassword(ftpConfigurationProvider.getPassword());
            Properties config = new java.util.Properties();
            config.put(STRING_HOST_KEY_CHECKING, NO);
            session.setConfig(config);
            session.connect();

            LOG.info("Host connected.");

            channel = session.openChannel(SFTP_PREFIX);
            channel.connect();
            LOG.info("sftp channel opened and connected.");
            channelSftp = (ChannelSftp) channel;
            channelSftp.cd(ftpConfigurationProvider.getRemotePath());
            String fileNameTemplate = new DateTime().toString(ftpConfigurationProvider.getFolderTemplate());
            String fileNameSaveToLocalDir = getFullPathToSaveZipArchive(fileNameTemplate);
            Vector<ChannelSftp.LsEntry> vector = channelSftp.ls(ftpConfigurationProvider.getRemotePath());
            Enumeration<ChannelSftp.LsEntry> entryEnumeration = vector.elements();
            String searchFile = fileNameTemplate + FILE_EXTENTION;

            String unzipDirToSaveFiles =
                    ftpConfigurationProvider.getLocalPath().endsWith(File.separator)
                    ? ftpConfigurationProvider.getLocalPath() + fileNameTemplate
                    : ftpConfigurationProvider.getLocalPath() + File.separator + fileNameTemplate;

            Stream<ChannelSftp.LsEntry> stream = Stream.of(vector.toArray(new ChannelSftp.LsEntry[0]));
            Optional<ChannelSftp.LsEntry> optional = stream.filter(

                    fileFtp -> (!fileFtp.getAttrs().isDir() && fileFtp.getFilename() != null && fileFtp.getFilename()
                            .equalsIgnoreCase(searchFile))

            ).findAny();
            if (optional.isPresent()) {
                ftpFileToLocalDir = new FileOutputStream(fileNameSaveToLocalDir);
                channelSftp.get(searchFile, ftpFileToLocalDir);
                ftpFileToLocalDir.close();
                unzipArchive(new File(fileNameSaveToLocalDir), new File(unzipDirToSaveFiles));

            }
        } catch (Exception ex) {

            LOG.error("Exception found while tranfer the response.", ex);
        } finally {
            try {
                if (ftpFileToLocalDir != null) {
                    ftpFileToLocalDir.close();
                }
            } catch (Exception e) {
                LOG.info("Stream was not closed.");
            }

            channelSftp.exit();
            LOG.info("sftp Channel exited.");
            channel.disconnect();
            LOG.info("Channel disconnected.");
            session.disconnect();
            LOG.info("Host Session disconnected.");
        }


    };
    FtpLoaderClient ftpLoaderClient = () -> {
        FTPClient ftpClient = new FTPClient();
        FileOutputStream outputStream1 = null;
        try {
            ftpClient.connect(ftpConfigurationProvider.getHost(), Integer.parseInt(ftpConfigurationProvider.getPort()));
            ftpClient.login(ftpConfigurationProvider.getLogin(), ftpConfigurationProvider.getPassword());
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(BINARY_FILE_TYPE);
            String fileNameTemplate = new DateTime().toString(ftpConfigurationProvider.getFolderTemplate());
            String localPathToSaveFiles = ftpConfigurationProvider.getLocalPath();
            String unzipDirTosaveFiles = localPathToSaveFiles + File.separator + fileNameTemplate;
            String localPathToSaveFileZip =
                    (localPathToSaveFiles.endsWith(File.separator)) ? localPathToSaveFiles
                                                                      + fileNameTemplate
                                                                      + FILE_EXTENTION
                                                                    : localPathToSaveFiles
                                                                      + File.separator
                                                                      + fileNameTemplate + FILE_EXTENTION;

            String fileSearchName = fileNameTemplate + FILE_EXTENTION;
            FTPFile[] ftpFiles = ftpClient.listFiles();
            Optional<FTPFile> optional = Stream.of(ftpFiles).filter(
                    ftpFile -> ftpFile.isFile() && ftpFile.getName().equals(fileSearchName)
            ).findAny();
            File zipFile = new File(localPathToSaveFileZip);
            if (optional.isPresent()) {
                outputStream1 = new FileOutputStream(localPathToSaveFileZip);
                boolean success = ftpClient.retrieveFile(zipFile.getName(), outputStream1);
                outputStream1.close();
                String postASuccessfulOperation = success ? "Copy was successful" : "Copy was failed";

                LOG.info(postASuccessfulOperation);
                unzipArchive(zipFile, new File(unzipDirTosaveFiles));
            }


        } catch (Exception e) {
            LOG.error("File could not copy  to local folder from FTP server", e);
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                }
                if (outputStream1 != null) {
                    outputStream1.close();
                }
            } catch (Exception e) {
                LOG.error("Problem with ftp log out", e);
            }

        }
    };

    private String getFullPathToSaveZipArchive(String fileNameTemplate) {
        String localPathToSaveFile = ftpConfigurationProvider.getLocalPath();

        if (localPathToSaveFile.endsWith(File.separator)) {
            return localPathToSaveFile + fileNameTemplate + FILE_EXTENTION;
        }
        return localPathToSaveFile + File.separator + fileNameTemplate + FILE_EXTENTION;
    }
}
