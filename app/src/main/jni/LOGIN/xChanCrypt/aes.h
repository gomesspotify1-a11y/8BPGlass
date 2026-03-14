#include "../openssl/evp.h"

namespace aes_crypt {
    using u8string = std::basic_string<uint8_t>;
    using u8string_view = std::basic_string_view<uint8_t>;

    class aes_encoder {
    public:
        std::size_t aes_encode(const uint8_t *key, const uint8_t *iv, uint8_t* buf, const uint8_t *data, size_t len) const {
            if (len == 0 || len == std::numeric_limits<size_t>::max())
                return 0;

            auto ret_result = 0, outlen = 0;

            auto ctx = EVP_CIPHER_CTX_new();
            if (!ctx)
                return 0;

            if (EVP_EncryptInit_ex(ctx, EVP_aes_256_cbc(), NULL, key, iv) != 1) {
                EVP_CIPHER_CTX_free(ctx);
                return 0;
            }

            if (EVP_EncryptUpdate(ctx, buf, &ret_result, data, len) != 1) {
                EVP_CIPHER_CTX_free(ctx);
                return 0;
            }

            if (EVP_EncryptFinal_ex(ctx, buf + ret_result, &outlen) != 1) {
                EVP_CIPHER_CTX_free(ctx);
                return 0;
            }

            ret_result += outlen;
            EVP_CIPHER_CTX_free(ctx);
            return ret_result;
        }

        std::size_t aes_decode(const uint8_t *key, const uint8_t *iv, uint8_t* buf, const uint8_t *data, size_t len) const {
            if (len == 0 || len == std::numeric_limits<size_t>::max())
                return 0;

            auto ret_result = 0, outlen = 0;

            auto ctx = EVP_CIPHER_CTX_new();
            if (!ctx)
                return 0;

            if (EVP_DecryptInit_ex(ctx, EVP_aes_256_cbc(), NULL, key, iv) != 1) {
                EVP_CIPHER_CTX_free(ctx);
                return 0;
            }

            if (EVP_DecryptUpdate(ctx, buf, &ret_result, data, len) != 1) {
                EVP_CIPHER_CTX_free(ctx);
                return 0;
            }

            if (EVP_DecryptFinal_ex(ctx, buf + ret_result, &outlen) != 1) {
                EVP_CIPHER_CTX_free(ctx);
                return 0;
            }

            ret_result += outlen;
            EVP_CIPHER_CTX_free(ctx);
            return ret_result;
        }
    };

    class AES256 {
    public:
        static aes_crypt::u8string Encrypt(const uint8_t *key, const uint8_t *iv, aes_crypt::u8string_view str) {
            using aes_crypt::aes_encoder;

            aes_encoder aes_enc{};

            if (key == nullptr || iv == nullptr)
                return u8string{};

            auto encoded_length = aes_enc.aes_encode(key, iv, aes_enc_buf.get(), str.data(), str.length());

            if (encoded_length != 0)
            {
                return u8string{aes_enc_buf.get(), encoded_length};
            }

            return u8string{};
        }

        static aes_crypt::u8string Decrypt(const uint8_t *key, const uint8_t *iv, aes_crypt::u8string_view str) {
            using aes_crypt::aes_encoder;

            aes_encoder aes_enc{};

            if (key == nullptr || iv == nullptr)
                return u8string{};

            auto decoded_length = aes_enc.aes_decode(key, iv, aes_dec_buf.get(), str.data(), str.length());

            if (decoded_length != 0)
            {
                return u8string{aes_dec_buf.get(), decoded_length};
            }

            return u8string{};
        }
    private:
        static thread_local inline auto aes_enc_buf{std::make_unique<uint8_t[]>(1024 * 1024 * 5)};
        static thread_local inline auto aes_dec_buf{std::make_unique<uint8_t[]>(1024 * 1024 * 5)};
    };
};
