package com.shopflow.unit.product;

import com.shopflow.common.error.DomainException;
import com.shopflow.product.domain.Product;
import com.shopflow.product.domain.ProductService;
import com.shopflow.product.domain.ProductStatus;
import com.shopflow.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 상품 서비스 단위 테스트 — FR-006~009. */
class ProductServiceTest {

    private final ProductRepository products = mock(ProductRepository.class);
    private final ProductService service = new ProductService(products);

    @Test
    void 등록시_가격_음수는_거부() {
        assertThatThrownBy(() -> service.register(1L, "사과", null, -100, 10, null))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void 등록시_이름_없으면_거부() {
        assertThatThrownBy(() -> service.register(1L, "  ", null, 100, 10, null))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void 타_판매자_상품_수정은_403() {
        Product owned = new Product(1L, "사과", null, 1000, 10, null);
        when(products.findById(5L)).thenReturn(Optional.of(owned));
        // 소유자는 sellerId=1인데 sellerId=2가 수정 시도
        assertThatThrownBy(() -> service.updateBasics(5L, 2L, "변경", null, 2000, null))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("본인 상품");
    }

    @Test
    void 본인_상품_판매중지_전환() {
        Product owned = new Product(1L, "사과", null, 1000, 10, null);
        when(products.findById(5L)).thenReturn(Optional.of(owned));
        Product result = service.changeStatus(5L, 1L, ProductStatus.OFF_SALE);
        assertThat(result.getStatus()).isEqualTo(ProductStatus.OFF_SALE);
    }

    @Test
    void 등록_성공() {
        when(products.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        Product p = service.register(1L, "사과", "맛있는 사과", 1500, 5, null);
        assertThat(p.getName()).isEqualTo("사과");
        assertThat(p.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }
}
