#include "../cpr/auth.h"

namespace cpr {
const char* Authentication::GetAuthString() const noexcept {
    return auth_string_.c_str();
}
} // namespace cpr


// curl_easy_setopt(curl, CURLOPT_PINNEDPUBLICKEY, "");