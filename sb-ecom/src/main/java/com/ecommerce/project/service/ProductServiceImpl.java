    package com.ecommerce.project.service;

    import com.ecommerce.project.exceptions.APIException;
    import com.ecommerce.project.exceptions.ResourceNotFoundException;
    import com.ecommerce.project.model.Category;
    import com.ecommerce.project.model.Product;
    import com.ecommerce.project.payload.ProductDTO;
    import com.ecommerce.project.payload.ProductResponse;
    import com.ecommerce.project.repositories.CategoryRepository;
    import com.ecommerce.project.repositories.ProductRepository;
    import org.modelmapper.ModelMapper;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Service;
    import org.springframework.web.multipart.MultipartFile;

    import java.io.File;
    import java.io.IOException;
    import java.nio.file.Files;
    import java.nio.file.Paths;
    import java.util.List;
    import java.util.UUID;


    @Service
    public class ProductServiceImpl implements ProductService{

        @Autowired
        private ProductRepository productRepository;

        @Autowired
        private CategoryRepository categoryRepository;

        @Autowired
        private ModelMapper modelMapper;

        @Autowired
        private FileService fileService;

        @Value("${project.image}")
        private String path;

        @Override
        public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category","categoryId",categoryId));
            Product product = modelMapper.map(productDTO, Product.class);
            product.setCategory(category);
            product.setImage("default.png");
            double specialPrice = product.getPrice() -
                    ((product.getDiscount()*0.01) * product.getPrice());
            product.setSpecialPrice(specialPrice);
            Product savedProduct = productRepository.save(product);
            return modelMapper.map(savedProduct,ProductDTO.class);
        }

        @Override
        public ProductResponse getAllProducts() {
            List<Product> productList = productRepository.findAll();
            List<ProductDTO> productDTOS =  productList.stream()
                    .map(product -> modelMapper.map(product,ProductDTO.class))
                    .toList();
            ProductResponse productResponse = new ProductResponse();
            productResponse.setContent(productDTOS);
            return productResponse;
        }

        @Override
        public ProductResponse getProductsByCategory(Long categoryId) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category","categoryId",categoryId));
            List<Product> products = productRepository.findByCategory(category);
            List<ProductDTO> productDTOS = products.stream()
                    .map(product -> modelMapper.map(product,ProductDTO.class))
                    .toList();
            ProductResponse productResponse = new ProductResponse();
            productResponse.setContent(productDTOS);
            return productResponse;
        }

        @Override
        public ProductResponse getProductByName(String productName) {
            List<Product> products = productRepository.findByProductNameLikeIgnoreCase('%' + productName + '%');
            List<ProductDTO> productDTOS = products.stream()
                    .map(product -> modelMapper.map(product,ProductDTO.class))
                    .toList();
            ProductResponse productResponse = new ProductResponse();
            productResponse.setContent(productDTOS);
            return productResponse;
        }

        @Override
        public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
            Product existingProduct = productRepository.findById(productId).
                    orElseThrow(() -> new APIException("Product does not exist"));
            Product product = modelMapper.map(productDTO, Product.class);
            existingProduct.setProductName(product.getProductName());
            existingProduct.setDescription(product.getDescription());
            existingProduct.setQuantity(product.getQuantity());
            existingProduct.setPrice(product.getPrice());
            existingProduct.setDiscount(product.getDiscount());
            double specialPrice = product.getPrice() -
                    ((product.getDiscount()*0.01) * product.getPrice());
            existingProduct.setSpecialPrice(specialPrice);
            Product savedProduct = productRepository.save(existingProduct);
            return modelMapper.map(savedProduct,ProductDTO.class);
        }

        @Override
        public ProductDTO deleteProduct(Long productId) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new APIException("Product not found"));
            productRepository.deleteById(productId);
            return modelMapper.map(product,ProductDTO.class);
        }

        @Override
        public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
            Product productFromDb = productRepository.findById(productId)
                                                .orElseThrow(()->new APIException("Product not found"));

            String fileName = fileService.uploadImage(path,image);
            productFromDb.setImage(fileName);
            Product savedProduct = productRepository.save(productFromDb);
            return modelMapper.map(savedProduct,ProductDTO.class);

        }


    }
