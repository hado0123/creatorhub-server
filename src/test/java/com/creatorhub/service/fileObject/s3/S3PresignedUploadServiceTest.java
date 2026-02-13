package com.creatorhub.service.fileObject.s3;

import com.creatorhub.constant.FileObjectStatus;
import com.creatorhub.constant.ThumbnailKeys;
import com.creatorhub.dto.fileUpload.s3.*;
import com.creatorhub.entity.FileObject;
import com.creatorhub.exception.fileUpload.s3.PresignedUrlIssueException;
import com.creatorhub.repository.FileObjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class S3PresignedUploadServiceTest {

    @Mock S3Presigner presigner;
    @Mock FileObjectRepository fileObjectRepository;

    private final String bucket = "test-bucket";
    private S3PresignedUploadService service;

    @BeforeEach
    void setUp() {
        service = new S3PresignedUploadService(presigner, fileObjectRepository, bucket);
    }

    @Test
    @DisplayName("썸네일 presigned 발급 성공: contentType 검증 -> FileObject 저장 -> presigned url 응답 반환")
    void generateThumbnailSuccess() throws Exception {
        PresignedPutRequest req = mock(PresignedPutRequest.class);
        given(req.contentType()).willReturn("image/jpeg");
        given(req.originalFilename()).willReturn("thumb.jpg");
        given(req.resolveSuffix()).willReturn(ThumbnailKeys.POSTER_SUFFIX);

        // repository.save 반환값(저장된 엔티티의 id 필요)
        FileObject saved = mock(FileObject.class);
        given(saved.getId()).willReturn(1L);
        given(fileObjectRepository.save(any(FileObject.class))).willReturn(saved);

        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        given(presigned.url()).willReturn(URI.create("https://example.com/presigned-put").toURL());
        given(presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presigned);

        ThumbnailPresignedUrlResponse resp = service.generatePresignedPutUrl(req);


        assertThat(resp.fileObjectId()).isEqualTo(1L);
        assertThat(resp.uploadUrl()).isEqualTo("https://example.com/presigned-put");
        assertThat(resp.storageKey()).isNotBlank();

        // storageKey 패턴 검증 (upload/yyyy/MM/dd/... + suffix)
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        assertThat(resp.storageKey()).startsWith("upload/" + datePath + "/");
        assertThat(resp.storageKey()).endsWith(ThumbnailKeys.POSTER_SUFFIX);

        // 저장된 FileObject의 내용 검증
        ArgumentCaptor<FileObject> foCaptor = ArgumentCaptor.forClass(FileObject.class);
        then(fileObjectRepository).should(times(1)).save(foCaptor.capture());

        FileObject toSave = foCaptor.getValue();
        assertThat(toSave.getStorageKey()).endsWith(ThumbnailKeys.POSTER_SUFFIX);
        assertThat(toSave.getContentType()).isEqualTo("image/jpeg");
        assertThat(toSave.getOriginalFilename()).isEqualTo("thumb.jpg");
        assertThat(toSave.getStatus()).isEqualTo(FileObjectStatus.INIT);
        assertThat(toSave.getSizeBytes()).isEqualTo(0L);

        // presigner에 들어간 요청 검증(버킷/키/콘텐트타입)
        ArgumentCaptor<PutObjectPresignRequest> presignCaptor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        then(presigner).should(times(1)).presignPutObject(presignCaptor.capture());

        PutObjectPresignRequest presignReq = presignCaptor.getValue();
        PutObjectRequest put = presignReq.putObjectRequest();
        assertThat(put.bucket()).isEqualTo(bucket);
        assertThat(put.key()).isEqualTo(resp.storageKey());
        assertThat(put.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("썸네일 presigned 발급 실패: contentType이 image/jpeg가 아니면 IllegalArgumentException")
    void generateThumbnailInvalidContentType() {
        PresignedPutRequest req = mock(PresignedPutRequest.class);
        given(req.contentType()).willReturn("image/png");

        assertThatThrownBy(() -> service.generatePresignedPutUrl(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("image/jpeg");

        then(fileObjectRepository).should(never()).save(any());
        then(presigner).should(never()).presignPutObject(Mockito.any(PutObjectPresignRequest.class));

    }

    @Test
    @DisplayName("썸네일 presigned 발급 실패: presigner 예외 발생 시 PresignedUrlIssueException")
    void generateThumbnailPresignFail() {
        PresignedPutRequest req = mock(PresignedPutRequest.class);
        given(req.contentType()).willReturn("image/jpeg");
        given(req.originalFilename()).willReturn("thumb.jpg");
        given(req.resolveSuffix()).willReturn(ThumbnailKeys.POSTER_SUFFIX);

        FileObject saved = mock(FileObject.class);
        given(fileObjectRepository.save(any(FileObject.class))).willReturn(saved);

        given(presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .willThrow(new RuntimeException());

        assertThatThrownBy(() -> service.generatePresignedPutUrl(req))
                .isInstanceOf(PresignedUrlIssueException.class);

        // save() 호출은 presign 이전에 수행됨(예외 시 트랜잭션 롤백으로 DB 커밋은 취소될 수 있음)
        then(fileObjectRepository).should(times(1)).save(any(FileObject.class));
    }

    @Test
    @DisplayName("원고 presigned 발급 성공: displayOrder 오름차순 정렬 + N개 저장 + N개 presigned URL 발급")
    void generateManuscriptsSuccess() throws Exception {

        // 2개의 원고 파일 생성
        ManuscriptFileRequest f2 = mock(ManuscriptFileRequest.class);
        given(f2.displayOrder()).willReturn(2);
        given(f2.originalFilename()).willReturn("2.jpg");
        given(f2.contentType()).willReturn("image/jpeg");

        ManuscriptFileRequest f1 = mock(ManuscriptFileRequest.class);
        given(f1.displayOrder()).willReturn(1);
        given(f1.originalFilename()).willReturn("1.jpg");
        given(f1.contentType()).willReturn("image/jpeg");

        ManuscriptPresignedRequest req = mock(ManuscriptPresignedRequest.class);
        given(req.creationId()).willReturn(99L);
        given(req.files()).willReturn(List.of(f2, f1)); // 일부러 섞어서 줌

        // saveAll은 저장된 리스트를 그대로 돌려준다고 가정
        // 여기서 id가 필요하니, saveAll이 반환하는 객체들은 getId()가 있어야 함.
        FileObject saved1 = mock(FileObject.class);
        given(saved1.getId()).willReturn(101L);
        FileObject saved2 = mock(FileObject.class);
        given(saved2.getId()).willReturn(102L);

        given(fileObjectRepository.saveAll(anyList())).willReturn(List.of(saved1, saved2));

        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        given(presigned.url()).willReturn(URI.create("https://example.com/put").toURL());
        given(presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presigned);

        // 원고 PresignedUrls 생성
        ManuscriptPresignedResponse resp = service.generateManuscriptPresignedUrls(req);

        // 원고 갯수 확인
        assertThat(resp.items()).hasSize(2);

        // displayOrder는 오름차순으로 응답돼야 함(1 -> 2)
        assertThat(resp.items().get(0).displayOrder()).isEqualTo(1);
        assertThat(resp.items().get(1).displayOrder()).isEqualTo(2);

        // storageKey는 manuscript suffix로 끝나야 함
        assertThat(resp.items().get(0).storageKey()).endsWith(ThumbnailKeys.MANUSCRIPT_SUFFIX);
        assertThat(resp.items().get(1).storageKey()).endsWith(ThumbnailKeys.MANUSCRIPT_SUFFIX);

        // presigner는 2번 호출
        then(presigner).should(times(2)).presignPutObject(any(PutObjectPresignRequest.class));

        // saveAll에 들어간 FileObject들이 order 001, 002 형태의 키를 갖는지(패턴 검증)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FileObject>> saveAllCaptor = ArgumentCaptor.forClass(List.class);
        then(fileObjectRepository).should(times(1)).saveAll(saveAllCaptor.capture());

        List<FileObject> toSave = saveAllCaptor.getValue();
        assertThat(toSave).hasSize(2);

        // 정렬 후 생성이므로 첫 번째는 order=1, 두 번째는 order=2
        assertThat(toSave.get(0).getStorageKey()).contains("/99/001_");
        assertThat(toSave.get(1).getStorageKey()).contains("/99/002_");

        assertThat(toSave.get(0).getContentType()).isEqualTo("image/jpeg");
        assertThat(toSave.get(1).getContentType()).isEqualTo("image/jpeg");

        assertThat(toSave.get(0).getStatus()).isEqualTo(FileObjectStatus.INIT);
        assertThat(toSave.get(1).getStatus()).isEqualTo(FileObjectStatus.INIT);
    }

    @Test
    @DisplayName("원고 presigned 발급 실패: displayOrder 중복이면 IllegalArgumentException")
    void generateManuscriptsDuplicateOrder() {

        ManuscriptFileRequest f1 = mock(ManuscriptFileRequest.class);
        given(f1.displayOrder()).willReturn(1);
        given(f1.contentType()).willReturn("image/jpeg");

        ManuscriptFileRequest f1dup = mock(ManuscriptFileRequest.class);
        given(f1dup.displayOrder()).willReturn(1);
        given(f1dup.contentType()).willReturn("image/jpeg");

        ManuscriptPresignedRequest req = mock(ManuscriptPresignedRequest.class);
        given(req.files()).willReturn(List.of(f1, f1dup));

        assertThatThrownBy(() -> service.generateManuscriptPresignedUrls(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("중복된 displayOrder");

        then(fileObjectRepository).should(never()).saveAll(anyList());
        then(presigner).should(never()).presignPutObject(Mockito.any(PutObjectPresignRequest.class));
    }

    @Test
    @DisplayName("원고 presigned 발급 실패: contentType이 image/jpeg가 아니면 IllegalArgumentException")
    void generateManuscriptsInvalidContentType() {
        ManuscriptFileRequest f1 = mock(ManuscriptFileRequest.class);
        given(f1.contentType()).willReturn("image/png");

        ManuscriptPresignedRequest req = mock(ManuscriptPresignedRequest.class);
        given(req.files()).willReturn(List.of(f1));

        assertThatThrownBy(() -> service.generateManuscriptPresignedUrls(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("image/jpeg");

        then(fileObjectRepository).should(never()).saveAll(anyList());
        then(presigner).should(never()).presignPutObject(Mockito.any(PutObjectPresignRequest.class));
    }
}
