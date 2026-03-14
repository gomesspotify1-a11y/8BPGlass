#include "../openssl/sha.h"

namespace xSHA {
    typedef unsigned char byte;
    using u8string = std::basic_string<uint8_t>;
    using u8string_view = std::basic_string_view<uint8_t>;

    u8string MD5_Encrypt(u8string_view msg) {
        auto hash_ctx = EVP_MD_CTX_new();
        std::uint32_t len = 16;
        u8string out;
        out.resize(16);

        EVP_DigestInit_ex(hash_ctx, EVP_md5(), nullptr);
        EVP_DigestUpdate(hash_ctx, msg.data(), msg.size());
        EVP_DigestFinal_ex(hash_ctx, (byte *) &out[0], &len);
        EVP_MD_CTX_free(hash_ctx);
        return out;
    }

    namespace SHA_512 {
        u8string Encrypt(u8string_view msg)
        {
            auto hash_ctx = EVP_MD_CTX_new();
            std::uint32_t len = 64;
            u8string out;
            out.resize(64);

            EVP_DigestInit_ex(hash_ctx, EVP_sha512(), nullptr);
            EVP_DigestUpdate(hash_ctx, msg.data(), msg.size());
            EVP_DigestFinal_ex(hash_ctx, (byte *) &out[0], &len);
            EVP_MD_CTX_free(hash_ctx);
            return out;
        }
    }

    namespace SHA_256 {
        u8string Encrypt(u8string_view msg)
        {
            auto hash_ctx = EVP_MD_CTX_new();
            std::uint32_t len = 32;
            u8string out;
            out.resize(32);

            EVP_DigestInit_ex(hash_ctx, EVP_sha256(), nullptr);
            EVP_DigestUpdate(hash_ctx, msg.data(), msg.size());
            EVP_DigestFinal_ex(hash_ctx, (byte *) &out[0], &len);
            EVP_MD_CTX_free(hash_ctx);
            return out;
        }
    }
}