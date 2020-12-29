package com.code4ro.legalconsultation.document.core.service.impl;

import com.code4ro.legalconsultation.comment.model.persistence.Comment;
import com.code4ro.legalconsultation.document.configuration.model.persistence.DocumentConfiguration;
import com.code4ro.legalconsultation.document.configuration.service.DocumentConfigurationService;
import com.code4ro.legalconsultation.document.consolidated.mapper.DocumentConsolidatedMapper;
import com.code4ro.legalconsultation.document.consolidated.model.dto.DocumentConsolidatedDto;
import com.code4ro.legalconsultation.document.consolidated.model.persistence.DocumentConsolidated;
import com.code4ro.legalconsultation.document.consolidated.service.DocumentConsolidatedService;
import com.code4ro.legalconsultation.document.core.service.DocumentService;
import com.code4ro.legalconsultation.document.export.model.DocumentExportFormat;
import com.code4ro.legalconsultation.document.export.service.DocumentExporter;
import com.code4ro.legalconsultation.document.export.service.DocumentExporterFactory;
import com.code4ro.legalconsultation.document.metadata.model.dto.DocumentMetadataDto;
import com.code4ro.legalconsultation.document.metadata.model.dto.DocumentViewDto;
import com.code4ro.legalconsultation.document.metadata.model.persistence.DocumentMetadata;
import com.code4ro.legalconsultation.document.metadata.service.DocumentMetadataService;
import com.code4ro.legalconsultation.document.node.model.persistence.DocumentNode;
import com.code4ro.legalconsultation.document.node.service.DocumentNodeService;
import com.code4ro.legalconsultation.mail.service.MailApi;
import com.code4ro.legalconsultation.pdf.mapper.PdfHandleMapper;
import com.code4ro.legalconsultation.pdf.model.dto.PdfHandleDto;
import com.code4ro.legalconsultation.pdf.service.PDFService;
import com.code4ro.legalconsultation.storage.service.StorageApi;
import com.code4ro.legalconsultation.user.mapper.UserMapper;
import com.code4ro.legalconsultation.user.model.dto.UserDto;
import com.code4ro.legalconsultation.user.model.persistence.User;
import com.code4ro.legalconsultation.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentConsolidatedService documentConsolidatedService;
    private final DocumentMetadataService documentMetadataService;
    private final PDFService pdfService;
    private final DocumentNodeService documentNodeService;
    private final StorageApi storageApi;
    private final UserService userService;
    private final MailApi mailService;
    private final UserMapper userMapperService;
    private final DocumentConsolidatedMapper documentConsolidatedMapper;
    private final PdfHandleMapper pdfHandleMapper;
    private final DocumentExporterFactory documentExporterFactory;
    private final DocumentConfigurationService documentConfigurationService;


    @Autowired
    public DocumentServiceImpl(final DocumentConsolidatedService documentConsolidatedService,
                               final DocumentMetadataService documentMetadataService,
                               final PDFService pdfService,
                               final DocumentNodeService documentNodeService,
                               final StorageApi storageApi,
                               final UserService userService,
                               final UserMapper userMapperService,
                               final DocumentConsolidatedMapper documentConsolidatedMapper,
                               final PdfHandleMapper pdfHandleMapper,
                               final DocumentExporterFactory documentExporterFactory,
                               final MailApi mailService,
                               final DocumentConfigurationService documentConfigurationService) {
        this.documentConsolidatedService = documentConsolidatedService;
        this.documentMetadataService = documentMetadataService;
        this.pdfService = pdfService;
        this.documentNodeService = documentNodeService;
        this.storageApi = storageApi;
        this.userService = userService;
        this.userMapperService = userMapperService;
        this.documentConsolidatedMapper = documentConsolidatedMapper;
        this.pdfHandleMapper = pdfHandleMapper;
        this.documentExporterFactory = documentExporterFactory;
        this.mailService = mailService;
        this.documentConfigurationService = documentConfigurationService;
    }

    @Transactional(readOnly = true)
    @Override
    public Page<DocumentMetadata> fetchAll(Pageable pageable) {
        return documentMetadataService.fetchAll(pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public DocumentMetadataDto fetchOne(final UUID id) {
        return documentMetadataService.fetchOne(id);
    }

    @Transactional(readOnly = true)
    @Override
    public DocumentConsolidatedDto fetchConsolidatedDtoByMetadataId(final UUID id) {
        final DocumentConsolidated document = documentConsolidatedService.getByDocumentMetadataId(id);
        return convertModelToDto(document);
    }

    @Transactional(readOnly = true)
    @Override
    public DocumentConsolidatedDto fetchConsolidatedDtoByDocumentNodeId(UUID id) {
        return convertModelToDto(documentConsolidatedService.getByMemberDocumentNodeId(id));
    }

    @Transactional(readOnly = true)
    @Override
    public DocumentConsolidated fetchConsolidatedByRootNodeId(UUID rootNodeId) {
        return documentConsolidatedService.getByDocumentNodeId(rootNodeId);
    }

    private DocumentConsolidatedDto convertModelToDto(DocumentConsolidated document) {
        return documentConsolidatedMapper.map(document);
    }

    @Transactional
    @Override
    public DocumentConsolidated create(final DocumentViewDto document) {

        DocumentMetadata metadata = documentMetadataService.build(document);
        metadata.setFilePath(document.getFilePath());
        final String pdfContent = pdfService.readAsString(storageApi.loadFile(document.getFilePath()));
        final DocumentNode documentNode = documentNodeService.parse(pdfContent);
        final DocumentConsolidated documentConsolidated = new DocumentConsolidated(metadata, documentNode);
        final DocumentConsolidated savedDocumentConsolidated =
                documentConsolidatedService.saveOne(documentConsolidated);

        final DocumentConfiguration documentConfiguration = new DocumentConfiguration(true, true);
        documentConfiguration.setDocumentConsolidated(documentConsolidated);
        documentConfigurationService.saveOne(documentConfiguration);

        return savedDocumentConsolidated;
    }

    @Transactional
    @Override
    public DocumentConsolidated update(final UUID id,
                                       final DocumentViewDto document) {
        final DocumentConsolidated consolidated = documentConsolidatedService.getEntity(id);

        // TODO delete current file from storage and the document node

        //update the metadata
        DocumentMetadata metadata = documentMetadataService.build(document);
        metadata.setFilePath(document.getFilePath());
        metadata.setId(consolidated.getDocumentMetadata().getId());

        final String pdfContent = pdfService.readAsString(storageApi.loadFile(document.getFilePath()));
        final DocumentNode documentNode = documentNodeService.parse(pdfContent);

        consolidated.setDocumentMetadata(metadata);
        consolidated.setDocumentNode(documentNode);
        return documentConsolidatedService.saveOne(consolidated);
    }

    @Transactional
    @Override
    public void deleteById(final UUID id) {
        documentConsolidatedService.getEntity(id);
        documentConsolidatedService.deleteById(id);
    }

    @Override
    public void assignUsers(final UUID id, final Set<UUID> userIds) {
        final List<User> users = userService.findByIds(userIds);
        final DocumentConsolidated documentConsolidated = documentConsolidatedService.getByDocumentMetadataId(id);
        documentConsolidated.setAssignedUsers(users);
        documentConsolidatedService.saveOne(documentConsolidated);

        mailService.sendDocumentAssignedEmail(documentConsolidated.getDocumentMetadata(), users);
    }

    @Override
    public List<UserDto> getAssignedUsers(final UUID id) {
        final DocumentConsolidated documentConsolidated = documentConsolidatedService.getByDocumentMetadataId(id);
        final List<User> assignedUsers = documentConsolidated.getAssignedUsers();

        return assignedUsers.stream().map(userMapperService::map).collect(Collectors.toList());
    }

    @Override
    public PdfHandleDto addPdf(final UUID id, final String state, final MultipartFile file) {
        final DocumentConsolidated documentConsolidated = documentConsolidatedService.getEntity(id);

        return pdfHandleMapper.map(pdfService.createPdf(documentConsolidated, state, file));
    }

    @Override
    @Transactional
    public byte[] export(final UUID id, final DocumentExportFormat type) {
        final DocumentExporter exporter = documentExporterFactory.getExporter(type);
        final DocumentConsolidated documentConsolidated = documentConsolidatedService.getEntity(id);
        return exporter.export(documentConsolidated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getAllAssignedDocumentsNodeIds(final User user) {
        final List<DocumentConsolidated> documents = documentConsolidatedService.getAllAssignedDocuments(user);
        return documents.stream()
                .flatMap(document -> document.getDocumentNode().flattened())
                .map(DocumentNode::getId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentConsolidated getDocumentConsolidatedForComment(Comment comment) {
        final DocumentNode rootNode = documentNodeService.findRootNodeForId(comment.getDocumentNode().getId());
        return fetchConsolidatedByRootNodeId(rootNode.getId());
    }
}
